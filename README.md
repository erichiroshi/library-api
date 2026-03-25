# 📚 Library API

![CI](https://github.com/erichiroshi/library-api/actions/workflows/ci.yml/badge.svg)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=library-api&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=library-api)
[![codecov](https://codecov.io/github/erichiroshi/library-api/graph/badge.svg?token=Y71AMP148X)](https://codecov.io/github/erichiroshi/library-api)

![Java](https://img.shields.io/badge/Java-25-red)
![Spring Boot 4](https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-microservices-blue)

Backend production-ready para gerenciamento de biblioteca, evoluindo de monolito para arquitetura de microservices.

---

## 🗂 Estrutura do Repositório

Este repositório adota estrutura **monorepo** com Gradle multi-project.
```
library-api/
├── library-api/       # Monolito original (v1.3.1) — Spring Boot 4 + JWT + Redis + S3
├── config-server/     # Spring Cloud Config Server — configuração centralizada
├── eureka-server/     # Netflix Eureka — service discovery
├── gateway/           # Spring Cloud Gateway — entrada única + JWT centralizado
├── auth-service/      # Autenticação, usuários e refresh tokens
├── catalog-service/   # Livros, autores, categorias e upload S3
├── loan-service/      # Empréstimos com Feign + Circuit Breaker
└── config-repo/       # Repositório de configurações (YAML por serviço)
```

---

## 🏛 Fase 2 — Monolito (v1.3.1)

Monolito production-ready com bounded contexts bem definidos, preparado para extração em microservices.

📁 [`library-api/`](./library-api)
📖 [Documentação completa do monolito](./library-api/readme.md)

**Stack:**
- Java 25 + Spring Boot 4.x
- PostgreSQL 16 + Flyway (schemas isolados: `auth`, `catalog`, `lending`)
- Redis 7 (cache distribuído)
- JWT + Refresh Token Rotation
- AWS S3 (upload de imagens com compressão automática)
- Resilience4j (Circuit Breaker + Retry)
- OpenTelemetry + Zipkin (tracing distribuído)
- Prometheus + Grafana (observabilidade)
- Testcontainers + JaCoCo (80%+ cobertura obrigatória)
- SonarCloud + Codecov + GitHub Actions

---

## 🔬 Fase 3 — Microservices (em desenvolvimento)

Extração do monolito em microservices usando o padrão **Strangler Fig**.

| Serviço | Descrição | Status |
|---|---|---|
| [`config-server/`](./config-server) | Configuração centralizada | ✅ Concluído |
| [`eureka-server/`](./eureka-server) | Service discovery | ✅ Concluído |
| [`gateway/`](./gateway) | API Gateway + JWT | ✅ Concluído |
| [`auth-service/`](./auth-service) | Autenticação | ✅ Concluído |
| [`catalog-service/`](./catalog-service) | Catálogo de livros | ✅ Concluído |
| [`loan-service/`](./loan-service) | Empréstimos | 🔲 Em breve |

**Decisões arquiteturais:**
- Monorepo com Gradle multi-project
- Spring Cloud Config (configuração centralizada)
- Netflix Eureka (service discovery com path para Kubernetes)
- Spring Cloud Gateway com JWT centralizado (downstream services confiam nos headers)
- OpenFeign + Resilience4j (comunicação entre serviços com Circuit Breaker)
- Um banco PostgreSQL com schemas isolados por serviço

> Acompanhe o progresso no [GitHub Projects](https://github.com/users/erichiroshi/projects/5)

---

## 🚀 Quick Start

Credenciais de teste:
- **ADMIN:** `joao.silva@email.com` / `123456`

### Microservices (parcial — em desenvolvimento)

Serviços disponíveis até o momento:

Rodar via CLI - perfil dev

```bash
# 1. Infraestrutra - Docker Compose (Postgres + Redis + PgAdmin)
cd library-api
docker compose -f docker-compose.dev.yml up -d

# 2. Config Server
cd config-server
./gradlew bootRun
# Acesse: http://localhost:8888/actuator/health

# 3. Eureka Server (requer Config Server rodando)
cd eureka-server
./gradlew bootRun --args='--spring.profiles.active=dev'
# Acesse: http://localhost:8761

# 4. Gateway (requer Config Server e Eureka Server rodando)
cd gateway
./gradlew bootRun --args='--spring.profiles.active=dev'
# Acesse: http://localhost:8080/actuator/health

# 5. Auth Service (requer Config Server, Eureka e Postgres)
cd auth-service
./gradlew bootRun --args='--spring.profiles.active=dev'
# Acesse: http://localhost:{port-spring}/actuator/health

# 6. Catalog Service (requer Config Server, Eureka e Postgres)
cd catalog-service
./gradlew bootRun --args='--spring.profiles.active=dev'
# Acesse: http://localhost:{port-spring}/actuator/health
```

> Docker Compose completo disponível ao final da Fase 3.

```bash
docker compose up -d
```

---

## Autor

**Eric Hiroshi** — Backend Engineer · Java / Spring Boot

[![LinkedIn](https://img.shields.io/badge/LinkedIn-Eric%20Hiroshi-blue)](https://www.linkedin.com/in/eric-hiroshi/)

[![GitHub](https://img.shields.io/badge/GitHub-erichiroshi-black)](https://github.com/erichiroshi)

---

## 📄 Licença

[MIT](LICENSE)