# Redis + Cache — Library API

## O que este documento cobre

Como o cache distribuído foi implementado com Redis: onde o cache foi aplicado, por que no nível de serviço e não de repositório, a estratégia de invalidação, o TTL, o delay artificial para demonstrar o efeito, e os trade-offs de cada decisão.

---

## Por que Cache?

O problema que o cache resolve no projeto é específico: a busca de livros é uma operação de leitura frequente e os dados mudam raramente. Sem cache, cada requisição `GET /books/{id}` vai ao banco, executa uma query, carrega a entidade, mapeia para DTO e retorna. Para centenas de requisições por segundo buscando os mesmos livros, isso é trabalho repetido desnecessariamente.

Com cache:

```
Primeira requisição:
GET /books/1 → Service → [cache miss] → Repository → PostgreSQL → armazena no Redis → retorna

Requisições subsequentes (até 2 min):
GET /books/1 → Service → [cache hit] → Redis → retorna
                                        ↑
                                   banco não é consultado
```

---

## Redis no Projeto

Redis é um armazenamento de dados em memória, usado aqui exclusivamente como cache. É externo à aplicação — roda como um container separado e é compartilhado entre todas as instâncias da aplicação.

Isso é relevante: se a aplicação escalar para múltiplas instâncias, todas compartilham o mesmo cache. Uma instância popula o cache, as outras se beneficiam. Isso é cache distribuído — diferente de cache local (em memória da JVM), que seria isolado por instância.

### Configuração

```yaml
# application.yml
spring:
  cache:
    type: redis
  data:
    redis:
      port: 6379
      timeout: 60000
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
```

O driver utilizado é o Lettuce (padrão do Spring Boot), que é thread-safe e suporta conexões reativas. O pool de conexões evita overhead de criar/destruir conexões a cada operação.

### CacheConfig

```java
@Configuration
@EnableCaching
@Profile("!it")  // desabilitado em testes de integração
public class CacheConfig {

    @Bean
    RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        return RedisCacheManager.builder(factory)
                .cacheDefaults(
                    RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(2)))
                .build();
    }
}
```

TTL global de 2 minutos: após esse tempo, a entrada expira automaticamente no Redis. Na próxima requisição, o cache miss dispara nova consulta ao banco e repovoа o cache.

**`@Profile("!it")`**: o cache é desabilitado no profile de testes de integração. Em testes, o comportamento desejado é sempre ir ao banco — cache interfere com assertivas e pode fazer um teste passar por dados cacheados de outro teste.

---

## Cache no Nível de Serviço

Uma decisão arquitetural importante: o cache foi aplicado nos métodos de `BookService`, não em `BookRepository`.

### Por que não no repositório?

Cache no repositório é tecnicamente possível, mas cria problemas:

- O repositório é uma abstração de dados — não tem conhecimento de regras de negócio
- Invalidar cache no repositório exige conhecimento de quando os dados mudam, o que é responsabilidade do serviço
- Testar fica mais difícil — o repositório tem responsabilidade dupla

Cache no serviço é mais limpo:

```
Controller → Service (cache aqui) → Repository → PostgreSQL
```

O serviço sabe exatamente quando dados mudam (create, delete) e pode invalidar o cache no momento certo.

### Outra vantagem

Cache no serviço funciona independente de como o dado é acessado — via REST, mensageria, scheduled job ou qualquer outra camada. O cache no repositório só protege o acesso via JPA.

---

## Os Dois Caches

O projeto usa dois caches distintos com estratégias diferentes:

### Cache `books` — Lista paginada

```java
@Cacheable(
    value = "books",
    key = "'size:' + #pageable.pageSize + ':sort:' + #pageable.sort",
    condition = "#pageable.pageNumber == 0"
)
public PageResponseDTO<BookResponseDTO> findAll(Pageable pageable) { ... }
```

**`value = "books"`**: nome do cache no Redis.

**`key`**: a chave inclui `pageSize` e `sort` — paginações diferentes geram entradas diferentes no cache. `GET /books?size=10&sort=title,asc` e `GET /books?size=20` são caches separados.

**`condition = "#pageable.pageNumber == 0"`**: só a primeira página é cacheada. Páginas seguintes (2, 3, ...) não são cacheadas — a premissa é que a primeira página é a mais acessada e as demais têm acesso esporádico. Cachear todas as páginas consumiria memória para dados raramente requisitados.

### Cache `bookById` — Busca por ID

```java
@Cacheable(value = "bookById", key = "#id")
public BookResponseDTO findById(Long id) {
    delayService.delay();  // delay artificial em dev
    return mapper.toDTO(find(id));
}
```

**`key = "#id"`**: cada livro tem sua própria entrada no cache. `book:1`, `book:2`, etc.

Dois caches separados permitem invalidação granular — ao deletar um livro, invalida apenas `bookById:{id}` sem afetar o cache da lista de outro livro.

---

## Estratégia de Invalidação

O cache é invalidado quando os dados mudam. A estratégia usada é **cache eviction** — ao criar ou deletar, as entradas relevantes são removidas. Na próxima leitura, o cache é repovoado com dados frescos do banco.

### Ao criar um livro

```java
@CacheEvict(value = "books", allEntries = true)
@Transactional
public BookResponseDTO create(BookCreateDTO dto) { ... }
```

`allEntries = true` invalida todas as entradas do cache `books` — necessário porque um novo livro pode aparecer em qualquer página e qualquer ordenação. Não é possível saber exatamente quais entradas do cache `books` seriam afetadas.

### Ao deletar um livro

```java
@Caching(evict = {
    @CacheEvict(value = "books", allEntries = true),
    @CacheEvict(value = "bookById", key = "#id")
})
@Transactional
public void deleteById(Long id) { ... }
```

Invalida ambos os caches: a lista (pois o livro sumiu da lista) e a entrada específica do livro deletado.

### Por que não cache de escrita (write-through)?

Write-through atualiza o cache no momento da escrita em vez de invalidar. Seria útil se houvesse operações de update frequentes. No projeto não há `PUT /books/{id}` — livros são criados e deletados, mas não editados via API. Eviction é a estratégia mais simples e correta para esse caso.

---

## O Delay Artificial

Esta é uma das decisões mais didáticas do projeto — e muito comum em demos técnicas.

### O problema de demonstrar cache

Cache é invisível para quem não tem acesso ao Redis ou aos logs. Sem algum indicador visual, é difícil perceber se o cache está funcionando.

### A solução

```java
// Interface — contrato
public interface ArtificialDelayService {
    void delay();
}

// Implementação dev — 2 segundos de sleep
@Profile("dev")
@Component
public class DevArtificialDelayService implements ArtificialDelayService {
    @Override
    public void delay() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }
}

// Implementação prod/test — não faz nada
@Profile("!dev")
@Component
public class NoOpArtificialDelayService implements ArtificialDelayService {
    @Override
    public void delay() {
        // no-op
    }
}
```

Com isso, `GET /books/{id}` em dev:
- **Primeira requisição**: 2 segundos (cache miss → vai ao banco)
- **Requisições seguintes**: instantâneo (cache hit → Redis)

A diferença é perceptível sem necessidade de ferramentas de monitoramento.

### Por que interface + duas implementações?

A alternativa seria um `if (isDev)` dentro do `BookService`. Mas isso polui a lógica de negócio com preocupação de ambiente. Com a interface, `BookService` não sabe qual implementação está ativa — o Spring injeta a correta via `@Profile`. Princípio de inversão de dependência aplicado a um caso simples.

---

## Cache e Serialização

Para armazenar no Redis, os objetos precisam ser serializados. O `BookResponseDTO` implementa `Serializable`:

```java
public record BookResponseDTO(
    Long id,
    String title,
    String isbn,
    Integer publicationYear,
    Integer availableCopies,
    Set<Long> authorIds,
    Long categoryId,
    String coverImageUrl
) implements Serializable {}
```

O `PageResponseDTO` também:

```java
public record PageResponseDTO<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) implements Serializable {}
```

Se o DTO não implementar `Serializable`, o Spring lança exceção ao tentar armazenar no Redis. Entidades JPA **nunca** devem ser cacheadas diretamente — além do problema de serialização (proxies lazy, referências circulares), o cache ficaria com o estado da entidade no momento do cache, sem refletir mudanças posteriores no banco.

---

## Cache e Consistência

O cache introduz uma janela de inconsistência: entre a invalidação e o repovoamento, dados podem ser ligeiramente defasados. Com TTL de 2 minutos, no pior caso um dado alterado diretamente no banco (fora da API) levaria até 2 minutos para refletir no cache.

Para o contexto do projeto — uma biblioteca onde livros não mudam a cada segundo — 2 minutos de TTL é aceitável. Em sistemas financeiros ou de inventário em tempo real, a estratégia seria diferente.

### Cache e o decrement de cópias

O decrement atômico de cópias (`availableCopies`) não passa pelo cache — é um `UPDATE` direto no banco via `@Modifying`. Isso significa que o cache de `bookById` pode ter o valor antigo de `availableCopies` após um empréstimo.

Essa inconsistência é aceitável no projeto porque:
1. O campo `availableCopies` é exibido na API mas não é usado para decisão de negócio na leitura — a decisão de disponibilidade é feita no `UPDATE WHERE availableCopies > 0` no banco
2. O TTL de 2 minutos limita a janela de defasagem
3. Invalidar `bookById` a cada empréstimo seria custoso e anularia o benefício do cache para um campo que muda com frequência

---

## Trade-offs do Redis como Cache

### Vantagens

- **Compartilhado entre instâncias**: escala horizontal sem problema de cache frio
- **TTL nativo**: expiração automática sem lógica adicional
- **Persistência opcional**: pode ser configurado para sobreviver a restarts (não usado aqui)
- **Velocidade**: operações em memória, latência de microsegundos

### Desvantagens

- **Dependência de infraestrutura**: Redis indisponível → todas as requisições vão ao banco (degradação, não falha total)
- **Consistência eventual**: window de inconsistência entre banco e cache
- **Serialização**: objetos precisam ser serializáveis, adiciona restrição ao design
- **Complexidade de invalidação**: estratégia de eviction precisa ser cuidadosamente pensada para cada caso

### E se o Redis cair?

O Spring Cache com Redis não tem fallback automático. Se o Redis cair, as operações de cache lançam exceção e a requisição falha — a menos que seja configurado um `CacheErrorHandler` personalizado para ignorar erros de cache e ir direto ao banco.

No projeto atual não há esse fallback implementado. Em produção real, o `CacheErrorHandler` seria uma adição importante para resiliência.

---

## Fase 3: Cache nos Microservices

Na transição para microservices, o cache permanece no `catalog-service` — que é o dono dos livros. O Redis pode ser compartilhado entre serviços ou cada serviço pode ter sua instância.

**Redis compartilhado**: mais simples, mas acopla os serviços na infraestrutura de cache. Uma mudança de configuração de cache em um serviço pode afetar outros.

**Redis por serviço**: mais isolado, alinhado com o princípio de autonomia dos microservices. Custo: mais instâncias para gerenciar.

Para o porte do projeto, Redis compartilhado é suficiente. A separação por serviço faria sentido em escala maior ou quando os requisitos de cache divergirem entre serviços.

O `loan-service` não precisaria de cache próprio inicialmente — empréstimos são operações com mais escrita que leitura, e o padrão de acesso não justifica cache no contexto atual.

---

## Resumo das Decisões de Cache

| Decisão | Alternativa | Por que esta |
|---|---|---|
| Cache no nível de serviço | Cache no repositório | Serviço conhece quando invalidar; cache funciona para qualquer camada consumidora |
| Dois caches separados (`books` e `bookById`) | Um cache único | Invalidação granular — deletar um livro não invalida o cache dos outros |
| Condition na lista (só página 0) | Cachear todas as páginas | Memória proporcional ao uso real; demais páginas têm acesso esporádico |
| TTL de 2 minutos | TTL maior ou sem TTL | Limita inconsistência; dados de livros mudam raramente |
| Eviction na escrita | Write-through | Sem endpoint de update, eviction é suficiente e mais simples |
| Interface para delay artificial | if/else no código | Sem polução da lógica de negócio; Spring injeta implementação correta por profile |
| DTOs serializáveis no cache | Entidades JPA | Evita proxies lazy e referências circulares; isola o cache do modelo de dados |
| Sem fallback de Redis | Com CacheErrorHandler | Simplificação consciente; adição importante para produção real |
