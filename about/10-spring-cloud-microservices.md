# Spring Cloud — Microservices (Fase 3)

## O que este documento cobre

A arquitetura de microservices planejada para a Fase 3: os sete componentes, Spring Cloud Config, Eureka, Spring Cloud Gateway com JWT centralizado, OpenFeign com Resilience4j, e as decisões de design da transição Strangler Fig a partir do monolito v1.3.1.

---

## Contexto: Por que Microservices Agora

O monolito v1.3.1 já foi projetado para facilitar essa transição:

- **Bounded contexts bem definidos**: auth, catalog, lending já são módulos coesos sem dependências cruzadas no nível de código
- **Schema per Service**: cada contexto já tem seu próprio schema PostgreSQL — a separação de banco já está feita
- **Anticorrupção via LookupServices**: os contextos se comunicam por interfaces, não por acesso direto a repositórios de outro contexto
- **Spring Events com primitivos**: eventos entre contextos usam apenas tipos primitivos — sem serialização de entidades complexas

A Fase 3 não é uma reescrita. É uma extração — o código de negócio dos bounded contexts migra para serviços independentes com o mínimo de mudança.

---

## Os Sete Componentes

```
config-repo          → repositório Git com configurações externas
config-server        → serve as configurações para todos os serviços
eureka-server        → service registry e discovery
gateway              → ponto de entrada único (JWT, roteamento, LB)
auth-service         → autenticação, usuários, tokens
catalog-service      → livros, autores, categorias, capas
loan-service         → empréstimos, devoluções, renovações
```

### Dependências de inicialização

```
config-repo (Git)
      ↓
config-server       ← sobe primeiro
      ↓
eureka-server       ← registra-se no config-server
      ↓
gateway             ← registra-se no Eureka
auth-service        ← registra-se no Eureka
catalog-service     ← registra-se no Eureka
loan-service        ← registra-se no Eureka
```

O `config-server` precisa estar disponível antes de todos os outros — os serviços buscam sua configuração nele durante o startup. Se o `config-server` não estiver acessível, o serviço falha ao iniciar.

---

## Spring Cloud Config

### O problema que resolve

Sem configuração centralizada, cada microservice tem seu próprio `application.yml` com valores como URLs de banco, TTLs de cache, credenciais de serviços externos. Mudar o TTL do Redis exigiria atualizar, rebuildar e reimplantar cada serviço individualmente.

Com Spring Cloud Config, as configurações ficam em um repositório Git externo (`config-repo`). Os serviços buscam sua configuração no `config-server` durante o startup — e podem recarregar configurações sem restart via Spring Cloud Bus.

### Estrutura do config-repo

```
config-repo/
├── application.yml              # configurações compartilhadas por todos
├── application-prod.yml         # compartilhadas em produção
├── auth-service.yml             # específicas do auth-service
├── auth-service-prod.yml        # auth-service em produção
├── catalog-service.yml
├── catalog-service-prod.yml
├── loan-service.yml
└── loan-service-prod.yml
```

O Spring Cloud Config aplica as configurações em ordem de precedência: `application.yml` → `application-{profile}.yml` → `{service}.yml` → `{service}-{profile}.yml`. Configurações mais específicas sobrescrevem as mais genéricas.

### Configuração do config-server

```yaml
# config-server/application.yml
spring:
  cloud:
    config:
      server:
        git:
          uri: ${CONFIG_REPO_URI}      # URL do repositório Git
          default-label: main
          search-paths: '{application}'  # pasta por serviço, opcional
  security:
    user:
      name: ${CONFIG_SERVER_USER}
      password: ${CONFIG_SERVER_PASSWORD}
```

O `config-server` expõe as configurações via HTTP — e é protegido por Basic Auth para evitar que configurações sensíveis sejam acessadas publicamente.

### Como os serviços buscam configuração

```yaml
# catalog-service/bootstrap.yml
spring:
  application:
    name: catalog-service       # define qual arquivo do config-repo usar
  config:
    import: configserver:http://config-server:8888
  cloud:
    config:
      username: ${CONFIG_SERVER_USER}
      password: ${CONFIG_SERVER_PASSWORD}
      fail-fast: true           # falha no startup se config-server inacessível
      retry:
        max-attempts: 6
        initial-interval: 1000
```

`fail-fast: true` com retry: o serviço tenta conectar ao `config-server` 6 vezes com intervalo crescente antes de desistir. Isso acomoda o tempo de startup do `config-server` sem tornar a falha silenciosa.

### O que fica no config-repo vs variáveis de ambiente

```yaml
# config-repo/catalog-service.yml — configurações que podem variar por ambiente
spring:
  cache:
    redis:
      time-to-live: 2m
  jpa:
    show-sql: false
aws:
  s3:
    bucket: library-catalog-bucket
    region: us-east-1

# NÃO vai para o config-repo — sempre via variável de ambiente
# aws.s3.access-key
# aws.s3.secret-key
# spring.datasource.password
```

Credenciais nunca vão para o `config-repo`, mesmo que seja um repositório privado. A separação é: configurações de comportamento vão para o config-repo; segredos vão para variáveis de ambiente ou um secrets manager (AWS Secrets Manager, HashiCorp Vault).

---

## Eureka — Service Registry

### O problema que resolve

Com serviços em containers, as URLs não são fixas. O `catalog-service` pode estar em `10.0.1.5:8082` agora e em `10.0.1.7:8082` após um restart. O `loan-service` não pode ter a URL do `catalog-service` hard-coded.

O Eureka é um service registry: cada serviço se registra com seu nome lógico ao subir, e outros serviços descobrem suas instâncias pelo nome — sem conhecer IPs ou portas.

```
catalog-service sobe → registra "catalog-service" no Eureka com IP:porta
loan-service quer chamar catalog-service → pergunta ao Eureka: "onde está catalog-service?"
Eureka responde: "10.0.1.5:8082"
loan-service chama diretamente
```

### Configuração do eureka-server

```yaml
# eureka-server/application.yml
server:
  port: 8761

eureka:
  instance:
    hostname: eureka-server
  client:
    register-with-eureka: false   # o próprio server não se registra
    fetch-registry: false         # não busca registro de si mesmo
```

### Configuração dos clientes

```yaml
# catalog-service.yml (via config-repo)
eureka:
  client:
    service-url:
      defaultZone: http://eureka-server:8761/eureka/
  instance:
    prefer-ip-address: true       # registra com IP em vez de hostname
    lease-renewal-interval: 10   # heartbeat a cada 10s
    lease-expiration-duration: 30 # removido do registro após 30s sem heartbeat
```

`prefer-ip-address: true` é importante em containers — o hostname do container não é resolvível externamente, mas o IP é.

### Eureka e múltiplas instâncias

Se o `catalog-service` escalar para 3 instâncias, todas se registram no Eureka com o mesmo nome lógico. O cliente Eureka (via Ribbon/Spring Cloud LoadBalancer) distribui as chamadas entre as instâncias automaticamente — round-robin por padrão.

---

## Spring Cloud Gateway

### O papel do Gateway

O Gateway é o único ponto de entrada externo para todos os microservices. Clientes externos nunca chamam `catalog-service` ou `loan-service` diretamente — tudo passa pelo Gateway.

```
Cliente
  │
  ↓
Gateway :8080
  │
  ├── /api/v1/auth/**     → auth-service
  ├── /api/v1/books/**    → catalog-service
  ├── /api/v1/authors/**  → catalog-service
  └── /api/v1/loans/**    → loan-service
```

### Roteamento via Eureka

```yaml
# gateway/application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: catalog-service
          uri: lb://catalog-service    # lb:// = load balancer via Eureka
          predicates:
            - Path=/api/v1/books/**, /api/v1/authors/**, /api/v1/categories/**

        - id: loan-service
          uri: lb://loan-service
          predicates:
            - Path=/api/v1/loans/**

        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/api/v1/auth/**
```

`lb://catalog-service` instrui o Gateway a resolver o endereço via Eureka e aplicar load balancing entre as instâncias disponíveis.

### JWT centralizado no Gateway

Esta é a decisão arquitetural mais importante do Gateway: **a validação de JWT acontece no Gateway, não em cada serviço**.

```
Cliente → Gateway
            │
            ├── valida JWT (assinatura, expiração, claims)
            │
            ├── extrai userId e roles do token
            │
            ├── adiciona headers: X-User-Id, X-User-Roles
            │
            └── repassa requisição para o serviço downstream
                  ↓
            catalog-service recebe X-User-Id e X-User-Roles
            (não precisa validar JWT — confia nos headers do Gateway)
```

#### Por que centralizar no Gateway?

Se cada serviço validasse JWT individualmente:
- Cada serviço precisaria da chave secreta JWT
- Mudança de algoritmo ou rotação de chave exigiria atualizar todos os serviços
- Lógica de segurança duplicada em N serviços

Com validação centralizada:
- Apenas o Gateway conhece a chave JWT
- Serviços downstream confiam nos headers internos
- Mudança de estratégia de autenticação afeta apenas o Gateway

#### Implementação no Gateway

```java
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = extractToken(exchange.getRequest());

        if (token == null) {
            return chain.filter(exchange);  // rotas públicas passam sem token
        }

        try {
            Claims claims = jwtService.validateAndExtract(token);

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", claims.getSubject())
                    .header("X-User-Roles", String.join(",", getRoles(claims)))
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (JwtException e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }
}
```

O Gateway usa WebFlux (reativo) — diferente dos serviços downstream que usam Spring MVC (imperativo). O filtro é assíncrono por natureza.

#### Serviços downstream: sem validação JWT, com autorização

```java
// catalog-service — lê os headers injetados pelo Gateway
@GetMapping("/books/{id}")
public ResponseEntity<BookResponseDTO> findById(
        @PathVariable Long id,
        @RequestHeader("X-User-Id") String userId,
        @RequestHeader("X-User-Roles") String roles) {
    // autorização local — verifica se o role tem permissão
    // sem validação JWT — confia que o Gateway já validou
}
```

A separação é: **autenticação** (quem é você?) no Gateway, **autorização** (o que você pode fazer?) em cada serviço.

---

## OpenFeign — Comunicação entre Serviços

### O problema

O `loan-service` precisa verificar se um livro existe e tem cópias disponíveis antes de criar um empréstimo — essa informação está no `catalog-service`. Serviços precisam se comunicar.

OpenFeign torna essa comunicação tão simples quanto uma interface Java:

```java
// loan-service — cliente Feign para o catalog-service
@FeignClient(name = "catalog-service")  // nome no Eureka
public interface CatalogServiceClient {

    @GetMapping("/api/v1/books/{id}")
    BookSummaryDTO getBook(@PathVariable Long id,
                           @RequestHeader("X-User-Id") String userId,
                           @RequestHeader("X-User-Roles") String roles);

    @PutMapping("/api/v1/books/{id}/decrement")
    void decrementAvailableCopies(@PathVariable Long id,
                                  @RequestHeader("X-User-Id") String userId,
                                  @RequestHeader("X-User-Roles") String roles);
}
```

O Feign resolve o endereço do `catalog-service` via Eureka, serializa/deserializa JSON, e propaga os headers de identidade — tudo automaticamente.

### Propagação de headers internos

Os headers `X-User-Id` e `X-User-Roles` injetados pelo Gateway precisam ser propagados nas chamadas Feign:

```java
@Configuration
public class FeignHeaderInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attrs =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String userId = request.getHeader("X-User-Id");
            String roles = request.getHeader("X-User-Roles");

            if (userId != null) template.header("X-User-Id", userId);
            if (roles != null) template.header("X-User-Roles", roles);
        }
    }
}
```

Sem isso, a chamada do `loan-service` para o `catalog-service` chegaria sem os headers de identidade — o `catalog-service` não saberia quem está fazendo a requisição.

### Feign + Resilience4j

```java
@FeignClient(
    name = "catalog-service",
    fallbackFactory = CatalogServiceFallbackFactory.class
)
public interface CatalogServiceClient { ... }
```

```java
@Component
public class CatalogServiceFallbackFactory
        implements FallbackFactory<CatalogServiceClient> {

    @Override
    public CatalogServiceClient create(Throwable cause) {
        return new CatalogServiceClient() {

            @Override
            public BookSummaryDTO getBook(Long id, String userId, String roles) {
                log.error("catalog-service unavailable for bookId={}", id, cause);
                throw new ServiceUnavailableException("Catalog service temporarily unavailable");
            }

            @Override
            public void decrementAvailableCopies(Long id, String userId, String roles) {
                // falha silenciosa — será compensada via Saga
                log.error("Failed to decrement copies for bookId={}", id, cause);
            }
        };
    }
}
```

`FallbackFactory` (em vez de `Fallback`) tem acesso à causa da falha — útil para logging diferenciado entre timeout, circuito aberto e erro HTTP.

---

## Transações Distribuídas e Saga Pattern

Na Fase 3, criar um empréstimo envolve duas operações em dois serviços:

1. `loan-service`: persiste o empréstimo no banco
2. `catalog-service`: decrementa `availableCopies`

Não existe transação ACID entre dois bancos de dados. Se o `catalog-service` falhar após o empréstimo ser criado no `loan-service`, os dados ficam inconsistentes.

### Saga Choreography

```
loan-service
  │ persiste empréstimo (status: PENDING)
  │
  └── publica evento: LoanCreatedEvent { loanId, bookId, userId }
            │
            ↓ (mensageria: Kafka ou RabbitMQ)
            │
      catalog-service
        │ recebe LoanCreatedEvent
        │ tenta decrementar availableCopies
        │
        ├── sucesso → publica BookDecrementedEvent
        │               └── loan-service recebe → atualiza status: ACTIVE
        │
        └── falha → publica BookUnavailableEvent
                      └── loan-service recebe → cancela empréstimo: CANCELLED
```

Cada serviço reage a eventos e publica o resultado. A consistência é eventual — há uma janela onde o empréstimo está `PENDING` enquanto o decremento não foi confirmado. O sistema converge para consistência sem coordenação centralizada.

A implementação completa do Saga fica para iterações posteriores da Fase 3 — o foco inicial é a extração dos serviços e a comunicação síncrona via Feign.

---

## Resumo das Decisões de Microservices

| Decisão | Alternativa | Por que esta |
|---|---|---|
| Strangler Fig (extração gradual) | Reescrita do zero | Menor risco; monolito continua funcionando durante a transição |
| Spring Cloud Config + Git | Configuração por variável de ambiente | Versionamento de config; mudança sem rebuild; centralização |
| Eureka | Consul / Kubernetes DNS | Ecossistema Spring nativo; simples para escala do projeto |
| JWT validado no Gateway | JWT validado em cada serviço | Segurança centralizada; serviços não precisam da chave JWT |
| Headers X-User-Id/X-User-Roles | Repassar o token JWT inteiro | Serviços downstream não precisam dependência de JWT; menor payload |
| OpenFeign | RestTemplate / WebClient | Interface declarativa; integração nativa com Eureka e Resilience4j |
| FallbackFactory | Fallback simples | Acesso à causa da falha para logging diferenciado |
| Saga Choreography | Saga Orchestration / 2PC | Sem coordenador central; desacoplamento; 2PC não escala em microservices |
| Monorepo (branch microservices) | Polyrepo (repo por serviço) | Mais simples para portfólio; refatorações cross-service em um commit |
