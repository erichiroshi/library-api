# Arquitetura Geral — Library API

## O que este documento cobre

Visão geral da arquitetura do projeto Library API: como o sistema está organizado hoje (monolito modular), as decisões que foram tomadas pensando na transição para microservices, os fluxos principais, e os trade-offs de cada escolha.

---

## Estado Atual: Monolito Modular

O projeto não é um monolito tradicional. É um **monolito modular** — uma aplicação única que internamente está organizada como se fossem serviços separados, com fronteiras bem definidas entre domínios.

A distinção é importante:

- **Monolito tradicional**: tudo misturado, qualquer camada acessa qualquer outra diretamente, difícil de separar no futuro.
- **Monolito modular**: fronteiras explícitas entre domínios, comunicação via contratos (interfaces), preparado para ser extraído.

O Library API foi construído já pensando na extração. Cada decisão arquitetural tem uma justificativa ligada a essa transição futura.

---

## Os Três Bounded Contexts

O domínio foi dividido em três contextos de negócio:

```
┌─────────────────────────────────────────────────────────┐
│                    Library API (Monolito)               │
│                                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │     AUTH     │  │   CATALOG    │  │   LENDING    │   │
│  │              │  │              │  │              │   │
│  │ - User       │  │ - Book       │  │ - Loan       │   │
│  │ - Roles      │  │ - Author     │  │ - LoanItem   │   │
│  │ - RefreshTkn │  │ - Category   │  │              │   │
│  └──────────────┘  └──────────────┘  └──────────────┘   │
│                                                         │
│              PostgreSQL (3 schemas)                     │
└─────────────────────────────────────────────────────────┘
```

| Contexto | Responsabilidade | Schema no banco |
|---|---|---|
| `auth` | Autenticação, usuários, tokens | `auth` |
| `catalog` | Livros, autores, categorias | `catalog` |
| `lending` | Empréstimos e itens | `lending` |

### Por que separar em bounded contexts?

Cada contexto tem:
- **Linguagem própria**: "livro" no catálogo tem ISBN, cópias, autores. "livro" no empréstimo é apenas uma referência com quantidade.
- **Ciclo de vida independente**: um livro existe antes de qualquer empréstimo. Um empréstimo referencia um livro mas não precisa de todos os seus dados.
- **Banco de dados separado logicamente**: cada schema é isolado. Nenhuma tabela de um schema cria FK para outro schema.

Essa separação hoje é só organizacional — tudo ainda está no mesmo processo e no mesmo banco. Mas o código está escrito como se fossem serviços separados.

---

## Schema Per Service

Uma das decisões mais importantes do projeto. As tabelas estão em schemas separados dentro do mesmo PostgreSQL:

```sql
-- Schema auth
auth.tb_user
auth.tb_user_roles
auth.tb_refresh_tokens

-- Schema catalog
catalog.tb_book
catalog.tb_author
catalog.tb_category
catalog.tb_book_author

-- Schema lending
lending.tb_loan
lending.tb_loan_item
```

### Por que isso importa?

**Regra fundamental de microservices**: cada serviço deve ser dono dos seus dados. Nenhum outro serviço pode acessar diretamente o banco de outro.

Se as tabelas estivessem todas no schema `public`, misturadas, a extração para microservices exigiria uma migração de dados custosa e arriscada. Com schemas separados, a migração futura é simples: cada serviço aponta para seu próprio PostgreSQL.

### Como funciona na prática hoje

O Hibernate resolve automaticamente porque o `search_path` está configurado:

```yaml
# application.yml
datasource:
  hikari:
    connection-init-sql: SET search_path TO auth,catalog,lending,public
```

As entidades anotam o schema explicitamente:

```java
@Table(name = "tb_book", schema = "catalog")
@Table(name = "tb_user", schema = "auth")
@Table(name = "tb_loan", schema = "lending")
```

### Trade-off

**Vantagem**: fronteira de dados clara, migração futura simples.  
**Desvantagem**: não é possível usar FK entre schemas de forma trivial (e nem deve — essa restrição é intencional, força o isolamento).

---

## Pacotes Feature-Based

A estrutura de pacotes segue o modelo **feature-based** (por domínio), não o modelo em camadas:

```
com.example.library/
├── auth/
├── author/
├── aws/
├── book/
├── category/
├── loan/
├── refresh_token/
├── security/
└── user/
```

### Comparação com o modelo em camadas

**Modelo em camadas** (mais comum em projetos Java tradicionais):
```
controllers/
services/
repositories/
models/
```

**Modelo feature-based** (usado aqui):
```
book/
  BookController.java
  BookService.java
  BookRepository.java
  Book.java
  dto/
  mapper/
  exception/
```

### Por que feature-based?

No modelo em camadas, para extrair o domínio `book` para um microservice você precisaria coletar arquivos espalhados por vários diretórios. No modelo feature-based, o domínio é praticamente auto-contido — você copia a pasta e tem quase tudo que precisa.

---

## Anticorrupção Layer: LookupServices

Este é o ponto mais sofisticado da arquitetura atual. O problema a resolver era: como `LoanService` precisa de dados de `Book` e `User`, mas não pode importar `BookRepository` ou `UserRepository` diretamente?

Se importasse diretamente:
- `lending` teria dependência estrutural de `catalog` e `auth`
- Na extração para microservices, seria necessário reescrever `LoanService` do zero

A solução foi criar interfaces de anticorrupção:

```java
// BookLookupService — interface no domínio book
public interface BookLookupService {
    Optional<Book> findById(Long id);
    int decrementCopies(Long id);
    void restoreCopies(Long id, int quantity);
}

// UserLookupService — interface no domínio user
public interface UserLookupService {
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
}
```

`LoanService` depende das interfaces, nunca das implementações:

```java
public class LoanService {
    private final BookLookupService bookAvailabilityPort;   // interface
    private final UserLookupService userLookupService;      // interface
}
```

### O que isso resolve na transição?

Hoje a implementação de `BookLookupService` consulta o `BookRepository` (banco local). Quando `catalog` virar um microservice separado, a implementação muda para uma chamada HTTP via Feign — mas `LoanService` não muda nada:

```
HOJE:
LoanService → BookLookupService → BookRepository → PostgreSQL

FUTURO:
LoanService → BookLookupService → BookClient (Feign) → HTTP → catalog-service
```

A interface é o contrato. A implementação é um detalhe que pode trocar.

---

## Comunicação Entre Domínios: Spring Events

Outro problema: quando um empréstimo é devolvido, as cópias do livro precisam ser restauradas. Mas `LoanService` não deve chamar `BookService` diretamente — isso criaria acoplamento.

A solução foi Spring Events (pub/sub interno):

```
LoanService.returnLoan()
    └── publica LoanReturnedEvent
            └── BookEventListener.onLoanReturned()
                    └── restaura cópias no banco
```

Os eventos carregam apenas dados primitivos — sem entidades JPA:

```java
public record LoanReturnedEvent(
    Long loanId,
    Long userId,
    Map<Long, Integer> bookQuantities  // bookId → quantidade
) {}
```

### Por que sem entidades JPA?

Quando migrar para mensageria (Kafka, RabbitMQ), o evento precisa ser serializável. Entidades JPA têm referências circulares, proxies lazy e outros problemas de serialização. Usando apenas primitivos, a mudança de `ApplicationEventPublisher` para Kafka é mínima.

### Trade-off

**Vantagem**: desacoplamento real entre domínios, fácil migração para mensageria.  
**Desvantagem**: o fluxo fica implícito — ao ler `LoanService`, não fica óbvio que o `BookEventListener` vai executar. Exige conhecimento do sistema para rastrear o fluxo completo.

---

## Camadas da Aplicação

```
┌─────────────────────────────────────────┐
│           Controllers (HTTP)            │  Recebe request, delega, retorna response
├─────────────────────────────────────────┤
│        DTOs / Request / Response        │  Contratos da API — nunca entidades JPA
├─────────────────────────────────────────┤
│        Services (Regra de Negócio)      │  Orquestra, valida, aplica regras
├─────────────────────────────────────────┤
│   LookupServices (Anticorrupção)        │  Contratos entre domínios
├─────────────────────────────────────────┤
│        Repositories (Dados)             │  Acesso ao banco
├─────────────────────────────────────────┤
│      PostgreSQL / Redis / AWS S3        │  Infraestrutura
└─────────────────────────────────────────┘
```

### Por que DTOs e não entidades diretamente?

Entidades JPA expostas diretamente na API causam problemas:
- **LazyInitializationException**: acessar uma coleção lazy fora da sessão JPA causa erro.
- **Vazamento de dados**: campos internos (senha, versão, auditoria) ficam expostos.
- **Acoplamento**: mudança na entidade quebra o contrato da API.

DTOs isolam completamente o modelo de dados do contrato da API. O MapStruct gera o código de mapeamento em tempo de compilação — sem reflection em runtime.

---

## Fluxo Completo: Criar Empréstimo

Para consolidar a visão arquitetural, um fluxo end-to-end:

```
1. POST /api/v1/loans
   │
2. JwtAuthenticationFilter
   │  → valida JWT, extrai roles do token (sem query ao banco)
   │  → popula SecurityContext com email + roles
   │
3. LoanController.create(dto)
   │  → delega para LoanService
   │
4. LoanService.create(dto)
   │  → getAuthenticatedUser() — lê email do SecurityContext, busca User
   │  → bookAvailabilityPort.findById() — carrega livros (BookLookupService)
   │  → bookAvailabilityPort.decrementCopies() — UPDATE atômico no banco
   │  → cria Loan + LoanItems
   │  → salva no banco
   │  → publica LoanCreatedEvent
   │
5. BookEventListener (não executado aqui — LoanCreatedEvent não altera cópias)
   │
6. Retorna LoanResponseDTO
```

O decrement atômico merece destaque:

```sql
UPDATE Book b
SET b.availableCopies = b.availableCopies - 1
WHERE b.id = :id
AND b.availableCopies > 0
```

Se duas requisições chegam ao mesmo tempo para o mesmo livro com apenas 1 cópia, o banco garante que apenas uma terá `rowsUpdated = 1`. A outra recebe `rowsUpdated = 0` e o sistema lança `BookNotAvailableException`. Sem locks explícitos, sem race condition.

---

## Fase 3: Transição para Microservices

### Estrutura planejada

```
library-api/
├── library-api/       ← monolito original (referência)
├── config-repo/       ← YAMLs centralizados por serviço
├── config-server/     ← Spring Cloud Config Server (porta 8888)
├── eureka-server/     ← Service Discovery (porta 8761)
├── gateway/           ← API Gateway + JWT Filter (porta 8080)
├── auth-service/      ← contexto auth (porta aleatória)
├── catalog-service/   ← contexto catalog (porta aleatória)
└── loan-service/      ← contexto lending (porta aleatória)
```

### O que muda na arquitetura

**Hoje (monolito)**:
- Uma JVM, um processo
- Comunicação entre domínios: chamada de método Java
- Transação única abrange todos os domínios
- Falha de um domínio derruba toda a aplicação

**Futuro (microservices)**:
- Uma JVM por serviço, processos independentes
- Comunicação entre domínios: HTTP (Feign) ou mensageria
- Sem transação distribuída — consistência eventual
- Falha de um serviço pode ser isolada com Circuit Breaker

### O papel do Gateway

O Gateway centraliza a responsabilidade de autenticação:

```
HOJE (monolito):
Request → JwtAuthenticationFilter → Controller → Service

FUTURO (microservices):
Request → Gateway (valida JWT)
              ├── /auth/**    → auth-service (livre, sem filtro)
              ├── /api/v1/books/** → catalog-service
              └── /api/v1/loans/** → loan-service
              
              Gateway propaga headers:
              X-User-Id: 42
              X-User-Roles: ROLE_USER,ROLE_ADMIN
```

Cada microservice confia nos headers — não valida JWT novamente. Isso elimina a necessidade de cada serviço conhecer a chave JWT.

### O que facilita a transição (já preparado)

| Decisão tomada hoje | Como ajuda na transição |
|---|---|
| Schema per service | Cada serviço aponta para seu próprio PostgreSQL sem migração de dados |
| LookupServices (interfaces) | Troca implementação local por Feign sem alterar consumidor |
| Spring Events com primitivos | Troca `ApplicationEventPublisher` por Kafka sem alterar estrutura dos eventos |
| Feature-based packages | Cada pasta vira um projeto Maven/Gradle independente |
| DTOs sem entidades JPA | Contratos da API já isolados, sem mudança necessária |
| JWT sem query ao banco | Gateway pode validar JWT sem acesso ao banco de users |

### Trade-offs da transição

**O que se ganha:**
- Deployments independentes por serviço
- Escalabilidade granular (escalar só o catalog se necessário)
- Isolamento de falhas — Circuit Breaker protege loan-service se catalog cair
- Times independentes podem evoluir cada serviço

**O que se perde ou complica:**
- **Transações distribuídas**: hoje uma operação de empréstimo é ACID. Com microservices, decrementar cópias no catalog e criar o loan em lending são duas operações em dois bancos. Se catalog confirmar e lending falhar, há inconsistência. Solução: Saga pattern (mais complexidade).
- **Latência**: chamada de método Java (nanosegundos) vira chamada HTTP (milissegundos). Cada inter-serviço adiciona latência.
- **Debugging**: rastrear um erro que passa por 3 serviços é mais difícil. Por isso o OpenTelemetry + Zipkin já está implementado — traceId percorre toda a cadeia.
- **Overhead operacional**: 7 processos para gerenciar em vez de 1. Docker Compose, healthchecks, ordem de subida, configuração centralizada — tudo isso existe no roadmap justamente para mitigar isso.
- **Consistência eventual**: eventos assíncronos (Kafka) entregam eventualmente, não imediatamente. O sistema precisa ser projetado para tolerar janelas de inconsistência.

### Por que Eureka e não Kubernetes diretamente?

Kubernetes resolve service discovery de forma nativa, mas tem curva de aprendizado e overhead operacional alto. Eureka é mais simples de rodar localmente e em ambientes menores. A migração futura de Eureka para o service discovery do Kubernetes é relativamente simples — os clientes Feign funcionam com ambos via Spring Cloud LoadBalancer.

---

## Resumo das Decisões Arquiteturais

| Decisão | Alternativa considerada | Por que esta |
|---|---|---|
| Monolito modular | Microservices direto | Complexidade prematura; monolito modular entrega 80% dos benefícios com 20% da complexidade |
| Schema per service | Banco separado por domínio | Prematuro; schemas dão fronteiras sem overhead operacional |
| LookupServices | Injeção direta de repositórios | Acoplamento estrutural impediria extração futura |
| Spring Events | Kafka desde o início | Kafka é infraestrutura adicional; Events é suficiente no monolito e fácil de migrar |
| Feature-based packages | Pacotes em camadas | Extração futura exige código co-localizado |
| DTOs + MapStruct | Entidades diretas na API | Isolamento, segurança, sem LazyInitializationException |
| JWT sem query ao banco | UserDetails com query a cada request | Performance: elimina 1 query por request autenticado |
