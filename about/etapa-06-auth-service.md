# Etapa 6 — Auth Service

**Branch:** `microservices`  
**Commit:** `feat(auth-service): extrair contexto de autenticação do monolito`  
**Status:** ✅ Concluída

---

## O que foi feito

- Criado `auth-service/` via Spring Initializr (Spring Boot 4.0.4)
- Copiados domínios `auth/`, `user/`, `refresh_token/`, `security/` do monolito
- `SecurityConfig` adaptado — `permitAll()` pois Gateway já valida JWT
- `JwtAuthenticationFilter` removido — desnecessário no auth-service
- `JwtService` inicializado via construtor com `@Value` (sem `@PostConstruct`)
- Migrations Flyway para schema `auth` (4 migrations)
- `ShedLock` para `RefreshTokenCleanupJob`
- Seed de dev: `db/dev_migration/V1000__dev_seed_user_data.sql`
- Swagger adicionado — documentação interativa para portfólio
- `GlobalExceptionHandler` com `NoResourceFoundException` (404) e `HttpRequestMethodNotSupportedException` (405)
- Perfil `dev` no `config-repo/auth-service-dev.yml`
- Porta aleatória — registrado no Eureka como `auth-service`
- Adicionado ao `docker-compose.yml`

---

## Estrutura criada
```
auth-service/
├── src/main/java/com/example/authservice/
│   ├── AuthServiceApplication.java
│   ├── auth/                        ← AuthController + DTOs
│   ├── user/                        ← User, UserRepository, CustomUserDetailsService
│   │   └── InternalUserController   ← endpoints internos para loan-service
│   ├── refresh_token/               ← RefreshToken, RefreshTokenService, CleanupJob
│   ├── security/                    ← JwtService, SecurityConfig (sem filtro JWT)
│   ├── swagger/                     ← OpenApiConfig
│   └── common/                      ← GlobalExceptionHandler, BaseEntity
├── src/main/resources/
│   ├── application.yml              ← apenas spring.application.name
│   ├── application-dev.yml          ← spring.config.import: configserver
│   └── db/
│       ├── migration/               ← V001-V004 schema auth
│       └── dev_migration/           ← V1000 seed dev
├── build.gradle
└── Dockerfile
```

---

## Decisões e Tradeoffs

### SecurityConfig com permitAll()

**Decisão:** `auth-service` não revalida JWT — confia nos headers do Gateway.

O Gateway valida o JWT e propaga `X-User-Id` e `X-User-Roles`. O `auth-service` não precisa de `JwtAuthenticationFilter`. Spring Security ainda é necessário para o `AuthenticationManager` usado no login.

**Risco:** se alguém acessar o `auth-service` diretamente (bypassando o Gateway), não há proteção. Mitigado em produção com regras de rede — apenas o Gateway pode chamar os serviços internos.

---

### Perfil dev separado

**Decisão:** `application-dev.yml` local + `auth-service-dev.yml` no `config-repo/`.

Fluxo de desenvolvimento:
1. `docker-compose.dev.yml` sobe postgres + redis + pgadmin
2. Config Server sobe local: `./gradlew bootRun`
3. Serviço sobe local: `./gradlew bootRun --args='--spring.profiles.active=dev'`

Sem `optional:` no `config.import` — Config Server é obrigatório. Sem ele, o serviço não sobe. Decisão intencional: forçar a ordem correta de inicialização.

---

### config.import sem optional

**Decisão:** `spring.config.import: configserver:http://localhost:8888` (sem `optional:`).

O monolito tinha fallback local com todas as propriedades. Os microservices não têm — todas as configurações de negócio ficam no `config-repo/`. Se o Config Server não estiver disponível, o serviço não deve subir.

---

### InternalUserController

Endpoints `/internal/users/{id}` e `/internal/users/by-email` criados para o `loan-service` chamar via Feign. Não expostos pelo Gateway — apenas comunicação interna entre serviços.

---

### JwtService via construtor
```java
public JwtService(@Value("${jwt.secret-key}") String secret) {
    if (secret == null || secret.trim().isEmpty()) {
        throw new IllegalStateException("SECRET_KEY não pode ser null ou vazio.");
    }
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
}
```

Fail-fast no startup — se a secret key não estiver configurada, a aplicação não sobe.

---

### Migrations isoladas por serviço

Cada serviço tem suas próprias migrations — apenas as tabelas do seu schema. O `auth-service` gerencia apenas `auth.tb_user`, `auth.tb_user_roles` e `auth.tb_refresh_tokens`.

---

### ShedLock permanece em public

A tabela `shedlock` fica em `public` — é infraestrutura, não domínio. Todos os serviços que precisarem de lock distribuído usam a mesma tabela.

---

### spring-boot-starter-webmvc

`spring-boot-starter-web` é um alias que pode puxar dependências reativas em projetos Spring Cloud. Usar `spring-boot-starter-webmvc` garante a servlet stack explicitamente.

---

### GlobalExceptionHandler — novos handlers
```java
@ExceptionHandler(NoResourceFoundException.class)
// → 404 para rotas inexistentes

@ExceptionHandler(HttpRequestMethodNotSupportedException.class)  
// → 405 para métodos HTTP não suportados
```

Sem esses handlers, o Spring retorna respostas genéricas sem o padrão `ProblemDetail` do projeto.

---

## Arquivos criados/modificados

| Arquivo | Ação |
|---|---|
| `auth-service/` | Criado via Spring Initializr |
| `auth-service/build.gradle` | Ajustado — webmvc, JWT, ShedLock, Swagger, Lombok, MapStruct |
| `auth-service/src/main/resources/application.yml` | Criado — apenas spring.application.name |
| `auth-service/src/main/resources/application-dev.yml` | Criado — spring.config.import |
| `auth-service/src/main/resources/db/migration/` | Criado — V001-V004 |
| `auth-service/src/main/resources/db/dev_migration/` | Criado — V1000 seed dev |
| `auth-service/Dockerfile` | Criado |
| `config-repo/auth-service-dev.yml` | Criado — configurações dev |
| `docker-compose.yml` | Atualizado — auth-service adicionado |
| `docker-compose.dev.yml` | Criado — postgres + redis + pgadmin |
| `settings.gradle` (raiz) | Modificado — `include 'auth-service'` |
| `.github/dependabot.yml` | Modificado — entrada do auth-service |

---

## Próxima etapa

**Etapa 7 — Catalog Service**