> Este documento é a versão técnica da documentação geral do projeto,
> utilizada para geração do PDF via CI.

# Library API — Monorepo: Monolito + Microservices

Backend production-ready para gerenciamento de biblioteca, evoluindo de monolito
para arquitetura de microservices usando o padrão Strangler Fig.

- Java 25 + Spring Boot 4.x
- Spring Cloud (Config Server, Eureka, Gateway, OpenFeign)
- PostgreSQL 16 + Redis 7
- JWT centralizado no Gateway
- Resilience4j (Circuit Breaker + Retry)
- OpenTelemetry + Zipkin + Prometheus + Grafana

---

## Estrutura do Repositório
```
library-api/
├── library-api/       # Monolito original (v1.3.1)
├── config-server/     # Spring Cloud Config Server
├── eureka-server/     # Netflix Eureka (service discovery)
├── gateway/           # Spring Cloud Gateway + JWT centralizado
├── auth-service/      # Autenticacao, usuarios e refresh tokens
├── catalog-service/   # Livros, autores, categorias e upload S3
├── loan-service/      # Emprestimos com Feign + Circuit Breaker
└── config-repo/       # Repositorio de configuracoes (YAML por servico)
```

---

## Fase 2 — Monolito (v1.3.1)

Monolito production-ready com bounded contexts bem definidos, preparado
para extracao em microservices via padrão Strangler Fig.

### Stack

- Java 25 + Spring Boot 4.x
- PostgreSQL 16 + Flyway (schemas isolados: auth, catalog, lending)
- Redis 7 (cache distribuido, TTL 2min)
- JWT + Refresh Token Rotation
- AWS S3 (upload de imagens com compressao automatica)
- Resilience4j (Circuit Breaker + Retry para integracoes externas)
- ShedLock (lock distribuido nos scheduled jobs)
- OpenTelemetry + Zipkin (tracing distribuido com W3C Trace Context)
- Prometheus + Grafana (metricas + dashboards provisionados)
- Testcontainers + JaCoCo (80%+ cobertura obrigatoria no CI)
- SonarCloud + Codecov + GitHub Actions

### Bounded Contexts

| Contexto | Tabelas | Schema DB | Responsabilidade |
|---|---|---|---|
| auth | tb_user, tb_refresh_tokens | auth | Autenticacao, usuarios, tokens |
| catalog | tb_book, tb_author, tb_category | catalog | Livros, autores, categorias, S3 |
| lending | tb_loan, tb_loan_item | lending | Emprestimos, itens emprestados |

### Metricas

- ~8.000 linhas de codigo
- 125+ testes (unit + integration)
- 80%+ cobertura (JaCoCo)
- 30+ endpoints REST versionados (/api/v1)
- 4 workflows GitHub Actions (CI, Docker, Release, README PDF)
- 3 bounded contexts isolados por schema

---

## Fase 3 — Microservices (em desenvolvimento)

Extracao do monolito em microservices usando o padrao Strangler Fig.
Cada servico possui seu proprio banco (schema isolado), configuracao
centralizada e se registra no Eureka para descoberta dinamica.

### Decisoes Arquiteturais

**Monorepo com Gradle multi-project**
Facilita refatoracao gradual e compartilhamento de configuracoes de build.
Cada servico e independente mas vive no mesmo repositorio.

**Spring Cloud Config Server**
Configuracoes centralizadas em config-repo/. Cada servico busca sua
configuracao no startup. Mudancas sem rebuild de imagem.

**Netflix Eureka**
Service discovery dinamico com portas aleatorias. Path claro para
migracao futura para Kubernetes (basta trocar o client).

**Spring Cloud Gateway com JWT centralizado**
Valida o JWT uma unica vez na entrada. Propaga X-User-Id e X-User-Roles
nos headers. Servicos downstream confiam nos headers sem validar JWT.

**OpenFeign + Resilience4j**
Comunicacao HTTP entre servicos com Circuit Breaker. Se catalog-service
estiver fora, loan-service nao cascateia a falha.

**Database per Service (schemas isolados)**
Um PostgreSQL com schemas separados por servico. Fronteiras claras sem
complexidade operacional de multiplos bancos. Migracao futura requer
apenas apontar cada servico para seu proprio banco.

### Servicos

| Servico | Porta | Responsabilidade | Status |
|---|---|---|---|
| config-server | 8888 | Configuracao centralizada | Concluido |
| eureka-server | 8761 | Service discovery | Concluido |
| gateway | 8080 | Entrada unica + JWT | Concluido |
| auth-service | aleatoria | Autenticacao | Concluido |
| catalog-service | aleatoria | Catalogo de livros | Pendente |
| loan-service | aleatoria | Emprestimos | Pendente |

### Ordem de Subida
```
1. postgres + redis          infraestrutura
2. config-server             configuracoes
3. eureka-server             descoberta
4. auth-service              autenticacao
5. catalog-service           catalogo
6. loan-service              emprestimos
7. gateway                   entrada
8. prometheus + grafana + zipkin   observabilidade
```

---

## Quick Start

Credenciais de teste:
- ADMIN: joao.silva@email.com / 123456

### Microservices (em breve)
```bash
docker compose up -d
```

---

## Autor

Eric Hiroshi — Backend Engineer, Java / Spring Boot

- LinkedIn: https://www.linkedin.com/in/eric-hiroshi/
- GitHub: https://github.com/erichiroshi

---

## Licenca

Este projeto esta sob a licenca MIT.