# Etapa 8 — Loan Service

**Branch:** `microservices`  
**Commit:** `feat(loan-service): extrair contexto de empréstimos do monolito`  
**Status:** ✅ Concluída

---

## O que foi feito

- Criado `loan-service/` via Spring Initializr (Spring Boot 4.0.4)
- Copiado domínio `loan/` do monolito
- Implementados `BookClient` e `UserClient` (Feign) substituindo `LookupServices`
- Fallbacks com Circuit Breaker (Resilience4j)
- `LoanService` adaptado para ler usuário do header `X-User-Id`
- `InternalUserController` adicionado no `auth-service`
- Endpoints `decrement` e `restore` adicionados no `catalog-service`
- `feign-hc5` adicionado para suporte a `PATCH` via Feign
- Migrations Flyway para schema `lending`
- Observabilidade: Prometheus + Zipkin adicionados
- Porta aleatória — registrado no Eureka como `loan-service`
- Management port fixa: `8090` (prod), `8093` (dev)
- Adicionado ao `docker-compose.yml`

---

## Estrutura criada
```
loan-service/
├── src/main/java/com/example/loanservice/
│   ├── LoanServiceApplication.java      ← @EnableFeignClients
│   ├── loan/                            ← Loan, LoanService, LoanController, LoanRepository
│   ├── client/
│   │   ├── BookClient.java              ← Feign → catalog-service
│   │   ├── UserClient.java              ← Feign → auth-service
│   │   ├── dto/
│   │   │   ├── BookDTO.java
│   │   │   └── UserDTO.java
│   │   └── fallback/
│   │       ├── BookClientFallback.java  ← Circuit Breaker fallback
│   │       └── UserClientFallback.java
│   └── common/
│       ├── exception/                   ← GlobalExceptionHandler
│       └── entity/                      ← BaseEntity
├── src/main/resources/
│   ├── application.yml                  ← apenas spring.application.name
│   ├── application-dev.yml              ← spring.config.import
│   └── db/migration/                    ← V001-V002 schema lending
├── build.gradle
└── Dockerfile
```

---

## Decisões e Tradeoffs

### Feign Clients substituindo LookupServices

No monolito, `LookupServices` eram interfaces de anticorrupção que chamavam repositórios locais. Nos microservices, a troca foi direta — mesma interface, implementação via HTTP:

| Monolito | Loan Service |
|---|---|
| `BookLookupService` | `BookClient` (Feign → catalog-service) |
| `UserLookupService` | `UserClient` (Feign → auth-service) |

O `LoanService` não precisou ser reescrito — apenas a injeção mudou.

---

### feign-hc5 para PATCH

O cliente HTTP padrão do Feign (`HttpURLConnection`) não suporta `PATCH`. Sem `feign-hc5`:
```
feign.RetryableException: Invalid HTTP method: PATCH
```

Solução:
```groovy
implementation 'io.github.openfeign:feign-hc5'
```

**Regra estabelecida:** todo serviço com Feign usando `PATCH` ou `PUT` inclui `feign-hc5`.

---

### Usuário via header X-User-Id

O monolito lia o usuário do `SecurityContextHolder`. O `loan-service` não tem Spring Security — o header `X-User-Id` (email) é propagado pelo Gateway após validação do JWT:
```java
@PostMapping
public ResponseEntity<LoanResponseDTO> create(
        @Valid @RequestBody LoanCreateDTO dto,
        @RequestHeader("X-User-Id") String userEmail) {
    return ResponseEntity.created(...).body(service.create(dto, userEmail));
}
```

---

### Circuit Breaker nos Feign Clients
```java
@FeignClient(name = "catalog-service", fallback = BookClientFallback.class)
```

Se o `catalog-service` estiver fora, o fallback loga o erro e retorna `Optional.empty()` — o `loan-service` lança `BookNotFoundException` de forma controlada, sem cascata de falhas.

---

### Endpoints internos nos outros serviços

**auth-service:** `/internal/users/{id}` e `/internal/users/by-email`  
**catalog-service:** `/api/v1/books/{id}/decrement` e `/api/v1/books/{id}/restore/{quantity}`

Não expostos pelo Gateway — apenas comunicação interna entre serviços via Eureka.

---

## Arquivos criados/modificados

| Arquivo | Ação |
|---|---|
| `loan-service/` | Criado via Spring Initializr |
| `loan-service/build.gradle` | Ajustado — Feign, feign-hc5, Resilience4j, Prometheus, Zipkin |
| `loan-service/src/main/resources/db/migration/` | Criado — V001-V002 |
| `loan-service/Dockerfile` | Criado |
| `auth-service/.../InternalUserController.java` | Criado |
| `catalog-service/.../BookController.java` | Modificado — endpoints decrement/restore |
| `catalog-service/.../BookService.java` | Modificado — métodos decrementCopies/restoreCopies |
| `config-repo/loan-service-dev.yml` | Criado — management.server.port: 8093 |
| `docker-compose.yml` | Atualizado — loan-service adicionado |
| `settings.gradle` (raiz) | Modificado — `include 'loan-service'` |
| `.github/dependabot.yml` | Modificado — entrada do loan-service |
