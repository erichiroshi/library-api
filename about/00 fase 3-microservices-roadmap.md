# Fase 3 — Extração de Microservices

> Branch: `microservices` (saindo de `v1.3.1`)  
> Estratégia: Strangler Fig — extração gradual, monolito permanece funcional na `main`

---

## Arquitetura Final

```
Client
  ↓
gateway:8080  ←  único ponto de entrada (valida JWT)
  ↓
eureka-server ←  service discovery (resolve nome → instância)
  ↓
auth-service:0      porta aleatória
catalog-service:0   porta aleatória
loan-service:0      porta aleatória

config-server ←  configurações centralizadas (todos os serviços consultam na subida)
config-repo   ←  YAMLs versionados no Git
```

---

## Estrutura do Repositório

```
library-api/
├── library-api/          monolito original (referência)
├── config-repo/          YAMLs centralizados por serviço
├── config-server/        Spring Cloud Config Server
├── eureka-server/        Spring Cloud Netflix Eureka
├── gateway/              Spring Cloud Gateway + JWT Filter
├── auth-service/         contexto auth extraído
├── catalog-service/      contexto catalog extraído
├── loan-service/         contexto lending extraído
└── docker-compose.yml    orquestra todos os serviços
```

---

## Fluxo de Autenticação

```
POST /auth/login
  → gateway (livre, sem filtro JWT)
  → auth-service (autentica, gera JWT)
  → retorna access_token + refresh_token ao client

Qualquer outro endpoint
  → gateway (GatewayFilter valida JWT)
      ├── inválido → 401 (gateway rejeita, serviço nunca vê)
      └── válido   → propaga X-User-Id e X-User-Roles nos headers
                   → catalog-service / loan-service (confiam nos headers)
```

---

## Etapas

### Etapa 1 — Preparar branch e reestruturar repositório

- [x] Criar branch `microservices` a partir da tag `v1.3.1`
- [x] Criar estrutura de diretórios no repositório
- [x] Mover monolito para subdiretório `library-api/`
- [x] Atualizar `.gitignore` para cobrir todos os subprojetos
- [x] Commit: `chore: restructure repo for microservices extraction`

---

### Etapa 2 — Config Repo

- [x] Criar diretório `config-repo/`
- [x] Criar `application.yml` — propriedades comuns a todos os serviços
  - Eureka client config
  - Actuator endpoints
  - Logging pattern com traceId
- [x] Criar `config-server.yml`
- [x] Criar `eureka-server.yml`
- [x] Criar `gateway.yml`
  - Rotas por serviço
  - JWT secret
- [x] Criar `auth-service.yml`
  - DataSource (schema `auth`)
  - JWT config
  - Flyway locations
- [x] Criar `catalog-service.yml`
  - DataSource (schema `catalog`)
  - Redis config
  - Flyway locations
- [x] Criar `loan-service.yml`
  - DataSource (schema `lending`)
  - Feign clients (auth-service, catalog-service)
  - Flyway locations
- [x] Commit: `feat(config-repo): add centralized configuration files`

---

### Etapa 3 — Config Server

- [x] Criar projeto Spring Boot em `config-server/`
- [x] Dependências: `spring-cloud-config-server`
- [x] Anotar com `@EnableConfigServer`
- [x] Configurar backend Git apontando para `config-repo/` local
- [x] Porta fixa: `8888`
- [x] Adicionar ao `docker-compose.yml`
- [x] Testar: `GET http://localhost:8888/auth-service/default`
- [x] Commit: `feat(config-server): add Spring Cloud Config Server`

---

### Etapa 4 — Eureka Server

- [x] Criar projeto Spring Boot em `eureka-server/`
- [x] Dependências: `spring-cloud-starter-netflix-eureka-server`
- [x] Anotar com `@EnableEurekaServer`
- [x] Configurar como `standalone` (não se registra em si mesmo)
- [x] Porta fixa: `8761`
- [x] Buscar configuração do Config Server (`bootstrap.yml`)
- [x] Adicionar ao `docker-compose.yml`
- [x] Testar: `http://localhost:8761` — dashboard Eureka
- [x] Commit: `feat(eureka-server): add service discovery`

---

### Etapa 5 — Gateway

- [x] Criar projeto Spring Boot em `gateway/`
- [x] Dependências:
  - `spring-cloud-starter-gateway`
  - `spring-cloud-starter-netflix-eureka-client`
  - `spring-cloud-starter-config`
  - `jjwt` (validação JWT)
- [x] Configurar rotas no `gateway.yml`:
  - `/auth/**` → `auth-service` (sem filtro JWT)
  - `/api/v1/books/**` → `catalog-service`
  - `/api/v1/authors/**` → `catalog-service`
  - `/api/v1/categories/**` → `catalog-service`
  - `/api/v1/loans/**` → `loan-service`
- [x] Implementar `JwtAuthenticationFilter` (GatewayFilter)
  - Valida JWT
  - Propaga `X-User-Id` e `X-User-Roles` nos headers
  - Retorna 401 se inválido
- [x] Porta fixa: `8080`
- [x] Registrar no Eureka
- [x] Adicionar ao `docker-compose.yml`
- [x] Commit: `feat(gateway): add Spring Cloud Gateway with JWT validation filter`

---

### Etapa 6 — Auth Service

- [x] Criar projeto Spring Boot em `auth-service/`
- [x] Dependências:
  - `spring-boot-starter-web`
  - `spring-boot-starter-data-jpa`
  - `spring-boot-starter-security`
  - `spring-cloud-starter-netflix-eureka-client`
  - `spring-cloud-starter-config`
  - `jjwt`
  - `flyway`
  - `postgresql`
- [x] Copiar do monolito:
  - `auth/` — AuthController, DTOs
  - `user/` — User, UserRepository, UserService
  - `refresh_token/` — RefreshToken, RefreshTokenService, RefreshTokenCleanupJob
  - `security/` — JwtService, SecurityConfig
  - Migrations `V001` a `V003` (tabelas auth schema)
- [x] Remover dependência do `SecurityContextHolder` como fonte de User
  - Receber `X-User-Id` do header (propagado pelo gateway)
- [x] Porta: `0` (aleatória)
- [x] Registrar no Eureka com nome `auth-service`
- [x] Adicionar ao `docker-compose.yml`
- [ ] Testes unitários e de integração
- [x] Commit: `feat(auth-service): extract authentication context from monolith`

---

### Etapa 7 — Catalog Service

- [x] Criar projeto Spring Boot em `catalog-service/`
- [x] Dependências:
  - `spring-boot-starter-web`
  - `spring-boot-starter-data-jpa`
  - `spring-cloud-starter-netflix-eureka-client`
  - `spring-cloud-starter-config`
  - `spring-boot-starter-cache` + Redis
  - `flyway`
  - `postgresql`
  - `aws-sdk` (S3 — BookMediaService)
- [x] Copiar do monolito:
  - `book/` — Book, BookService, BookMediaService, BookRepository
  - `author/` — Author, AuthorService, AuthorRepository
  - `category/` — Category, CategoryService, CategoryRepository
  - `aws/` — S3Service
  - Migrations das tabelas catalog schema
- [x] Remover LookupServices — não há dependência de outros domínios
- [x] Ler `X-User-Id` e `X-User-Roles` do header (sem Spring Security completo)
- [x] Porta: `0` (aleatória)
- [x] Registrar no Eureka com nome `catalog-service`
- [x] Adicionar ao `docker-compose.yml`
- [ ] Testes unitários e de integração
- [x] Commit: `feat(catalog-service): extract catalog context from monolith`

---

### Etapa 8 — Loan Service

- [x] Criar projeto Spring Boot em `loan-service/`
- [x] Dependências:
  - `spring-boot-starter-web`
  - `spring-boot-starter-data-jpa`
  - `spring-cloud-starter-netflix-eureka-client`
  - `spring-cloud-starter-config`
  - `spring-cloud-starter-openfeign`
  - `resilience4j-spring-boot3`
  - `flyway`
  - `postgresql`
- [x] Copiar do monolito:
  - `loan/` — Loan, LoanService, LoanRepository, LoanItem
  - Migrations das tabelas lending schema
- [x] Implementar Feign clients:
  - `BookClient` → `catalog-service` (findById, decrementCopies, restoreCopies)
  - `UserClient` → `auth-service` (findById, findByEmail)
- [x] Substituir LookupServices pelos Feign clients
- [x] Ler `X-User-Id` e `X-User-Roles` do header
- [x] Circuit Breaker nos Feign clients (Resilience4j)
- [x] Porta: `0` (aleatória)
- [x] Registrar no Eureka com nome `loan-service`
- [x] Adicionar ao `docker-compose.yml`
- [ ] Testes unitários e de integração
- [x] Commit: `feat(loan-service): extract lending context from monolith`

---

### Etapa 9 — Docker Compose Final

- [x] Criar `docker-compose.yml` na raiz orquestrando:
  - `postgres` (único banco, schemas separados)
  - `redis`
  - `config-server`
  - `eureka-server` (depends_on: config-server)
  - `gateway` (depends_on: eureka-server)
  - `auth-service` (depends_on: eureka-server)
  - `catalog-service` (depends_on: eureka-server)
  - `loan-service` (depends_on: eureka-server)
  - `prometheus` + `grafana` + `zipkin`
- [x] Healthchecks em todos os serviços
- [x] Commit: `chore(docker): add full microservices docker-compose`

---

### Etapa 10 — CI/CD atualizado

- [ ] Atualizar `ci.yml` para rodar testes em todos os subprojetos
- [ ] Atualizar `docker.yml` para buildar imagem de cada serviço
- [ ] Atualizar `release.yml` para taggear todas as imagens
- [ ] Commit: `ci: update workflows for microservices structure`

---

### Etapa 11 — Merge e Release

- [ ] Abrir PR `microservices` → `main`
- [ ] Validar CI completo
- [ ] Merge na `main`
- [ ] Tag `v2.0.0`
- [ ] Commit: `chore(release): bump version to v2.0.0`

---

## Ordem de Subida dos Serviços

```
1. postgres + redis          infraestrutura
2. config-server             configurações
3. eureka-server             descoberta
4. auth-service              autenticação
5. catalog-service           catálogo
6. loan-service              empréstimos
7. gateway                   entrada
8. prometheus + grafana      observabilidade
```

---

## Checklist de Definições

- [x] Monorepo — histórico linear no Git
- [x] Branch `microservices` saindo de `v1.3.1`
- [x] Portas aleatórias (`server.port=0`) — descoberta via Eureka
- [x] Config Server com backend Git (`config-repo/`)
- [x] Autenticação JWT centralizada no Gateway
- [x] Serviços internos confiam em headers `X-User-Id` e `X-User-Roles`
- [x] Spring Cloud Gateway como único ponto de entrada
- [x] Feign + Resilience4j para comunicação entre serviços
- [x] Banco único PostgreSQL com schemas separados (auth, catalog, lending)
- [x] Migração futura para Kubernetes sem mudança de contratos
