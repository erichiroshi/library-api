# Etapa 7 — Catalog Service

**Branch:** `microservices`  
**Commit:** `feat(catalog-service): extrair contexto de catálogo do monolito`  
**Status:** ✅ Concluída

---

## O que foi feito

- Criado `catalog-service/` via Spring Initializr (Spring Boot 4.0.4)
- Copiados domínios `book/`, `author/`, `category/`, `aws/` do monolito
- `LookupServices` removidos — sem dependência de outros domínios
- `BookService` adaptado para usar repositórios diretamente
- `UserContext` criado para ler headers `X-User-Id` e `X-User-Roles`
- Endpoints internos de `decrement` e `restore` adicionados no `BookController`
- Migrations Flyway para schema `catalog`
- Seed de dev: `db/dev_migration/V1000__dev_seed_catalog_data.sql`
- Redis cache configurado
- AWS S3 + Resilience4j mantidos
- Swagger adicionado
- `GlobalExceptionHandler` com handlers de 404 e 405
- Perfil `dev` no `config-repo/catalog-service-dev.yml`
- Porta aleatória — registrado no Eureka como `catalog-service`
- Adicionado ao `docker-compose.yml`

---

## Estrutura criada
```
catalog-service/
├── src/main/java/com/example/catalogservice/
│   ├── CatalogServiceApplication.java
│   ├── book/                        ← Book, BookService, BookController, BookRepository
│   │   └── BookMediaService         ← upload S3
│   ├── author/                      ← Author, AuthorService, AuthorController
│   ├── category/                    ← Category, CategoryService, CategoryController
│   ├── aws/                         ← S3Service, S3Config, ImageProcessingService
│   ├── swagger/                     ← OpenApiConfig
│   └── common/
│       ├── security/
│       │   └── UserContext.java     ← lê X-User-Id e X-User-Roles dos headers
│       ├── exception/               ← GlobalExceptionHandler
│       └── entity/                  ← BaseEntity
├── src/main/resources/
│   ├── application.yml              ← apenas spring.application.name
│   ├── application-dev.yml          ← spring.config.import: configserver
│   └── db/
│       ├── migration/               ← V001-V003 schema catalog
│       └── dev_migration/           ← V1000 seed dev
├── build.gradle
└── Dockerfile
```

---

## Decisões e Tradeoffs

### LookupServices removidos

No monolito, `BookService` dependia de `AuthorLookupService` e `CategoryLookupService` — interfaces de anticorrupção que facilitavam a extração. No `catalog-service`, todos os domínios (`book`, `author`, `category`) vivem no mesmo serviço — não há fronteira a cruzar. Os repositórios são injetados diretamente.

**Benefício do investimento anterior:** as interfaces de anticorrupção tornaram a remoção trivial — apenas troca de injeção, sem mudança de lógica.

---

### UserContext via @RequestScope

O `catalog-service` não tem Spring Security. Para operações que precisam da identidade do usuário:
```java
@Component
@RequestScope
public class UserContext {
    public String getUserId() {
        return request.getHeader("X-User-Id");
    }
    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }
}
```

`@RequestScope` garante uma instância por request — thread-safe e sem vazamento de contexto entre requests.

---

### spring.cloud.gateway.server.webflux.routes

O path correto das rotas no Spring Cloud Gateway mudou na versão atual:
```yaml
# Incorreto (versão anterior):
spring:
  cloud:
    gateway:
      routes:

# Correto:
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
```

A dependência também é explícita: `spring-cloud-starter-gateway-server-webflux`.

---

### Endpoints internos no BookController
```
PATCH /api/v1/books/{id}/decrement   ← loan-service chama via Feign
PATCH /api/v1/books/{id}/restore/{quantity}
```

Esses endpoints não são expostos pelo Gateway — apenas comunicação interna. Em produção, seriam protegidos por regras de rede ou um header `X-Internal-Request`.

---

### Seed de dev por serviço

Cada serviço tem seu próprio seed em `db/dev_migration/V1000__dev_seed_*.sql`. Flyway aplica automaticamente ao subir com perfil `dev` — banco populado para testes sem esforço manual.

---

## Arquivos criados/modificados

| Arquivo | Ação |
|---|---|
| `catalog-service/` | Criado via Spring Initializr |
| `catalog-service/build.gradle` | Ajustado — webmvc, Redis, S3, Resilience4j, Swagger |
| `catalog-service/src/main/resources/application.yml` | Criado — apenas spring.application.name |
| `catalog-service/src/main/resources/application-dev.yml` | Criado — spring.config.import |
| `catalog-service/src/main/resources/db/migration/` | Criado — V001-V003 |
| `catalog-service/src/main/resources/db/dev_migration/` | Criado — V1000 seed dev |
| `catalog-service/Dockerfile` | Criado |
| `config-repo/catalog-service-dev.yml` | Criado — configurações dev |
| `docker-compose.yml` | Atualizado — catalog-service adicionado |
| `settings.gradle` (raiz) | Modificado — `include 'catalog-service'` |
| `.github/dependabot.yml` | Modificado — entrada do catalog-service |

---

## Próxima etapa

**Etapa 8 — Loan Service**

- Feign Clients para `catalog-service` e `auth-service`
- Circuit Breaker nos Feign Clients
- Ler `X-User-Id` do header em vez de `SecurityContextHolder`
- Migrations Flyway para schema `lending`