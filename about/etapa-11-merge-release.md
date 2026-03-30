# Etapa 11 — Merge e Release v2.0.0

**Branch:** `microservices` → `main`  
**Commit:** `feat: Fase 3 — extração de microservices (v2.0.0)`  
**Tag:** `v2.0.0`  
**Status:** ✅ Concluída

---

## O que foi feito

- README.md atualizado com status final de todos os serviços
- readme_pdf.md atualizado com tabela de serviços completa
- PR aberto: `microservices` → `main`
- CI validado — todos os jobs passaram
- Merge na `main`
- Tag `v2.0.0` criada
- GitHub Release gerado automaticamente
- Imagens Docker tagueadas: `library-*:v2.0.0`

---

## Resumo da Fase 3

### Serviços criados

| Serviço | Porta servidor | Porta actuator | Responsabilidade |
|---|---|---|---|
| `config-server` | 8888 | 8888 | Configuração centralizada |
| `eureka-server` | 8761 | 8761 | Service discovery |
| `gateway` | 8080 | 8080 | API Gateway + JWT |
| `auth-service` | aleatória | 8090 | Autenticação |
| `catalog-service` | aleatória | 8090 | Catálogo + S3 + Redis |
| `loan-service` | aleatória | 8090 | Empréstimos + Feign |

### Decisões arquiteturais documentadas

| Decisão | Etapa | Motivo |
|---|---|---|
| Monorepo Gradle multi-project | 1 | Visibilidade e refatoração gradual |
| Config Server com perfil native | 2 | Zero infraestrutura extra local |
| Eureka com porta fixa 8761 | 3 | Ponto de descoberta precisa ser previsível |
| JWT centralizado no Gateway | 5 | Sem duplicação de lógica de segurança |
| permitAll() no auth-service | 6 | Gateway já validou — confia na rede interna |
| LookupServices → Feign Clients | 8 | Anticorrupção do monolito facilitou a troca |
| feign-hc5 para PATCH | 8 | HttpURLConnection não suporta PATCH |
| Management port 8090 | 9 | Actuator previsível para healthcheck e Prometheus |
| fail-fast: false no CI | 10 | Serviços independentes falham independentemente |

### Stack completa
```
Monolito (v1.3.1):
Java 25, Spring Boot 4.x, PostgreSQL 16, Redis 7,
JWT, AWS S3, Resilience4j, OpenTelemetry, Testcontainers

Microservices (v2.0.0):
+ Spring Cloud 2025.1.1
  Config Server, Eureka, Gateway (WebFlux), OpenFeign
+ Resilience4j Circuit Breaker nos Feign Clients
+ Management port separada (8090)
+ prometheus-dev.yml por ambiente
```

---

## Etapas concluídas

| Etapa | Descrição | Status |
|---|---|---|
| 1 | Reestruturação monorepo | ✅ |
| 2 | Config Repo | ✅ |
| 3 | Config Server | ✅ |
| 4 | Eureka Server | ✅ |
| 5 | Gateway | ✅ |
| 6 | Auth Service | ✅ |
| 7 | Catalog Service | ✅ |
| 8 | Loan Service | ✅ |
| 9 | Docker Compose Final + Observabilidade | ✅ |
| 10 | CI/CD atualizado | ✅ |
| 11 | Merge e Release v2.0.0 | ✅ |

---

## Imagens Docker publicadas
```bash
docker pull {user}/library-api:v2.0.0           # monolito
docker pull {user}/library-config-server:v2.0.0
docker pull {user}/library-eureka-server:v2.0.0
docker pull {user}/library-gateway:v2.0.0
docker pull {user}/library-auth-service:v2.0.0
docker pull {user}/library-catalog-service:v2.0.0
docker pull {user}/library-loan-service:v2.0.0
```
