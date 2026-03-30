# Etapa 10 — CI/CD atualizado

**Branch:** `microservices`  
**Commit:** `ci: atualizar workflows para estrutura de microservices`  
**Status:** ✅ Concluída

---

## O que foi feito

- `ci.yml` separado em `test-monolith` e `test-microservices` (matrix)
- `docker.yml` separado em `build-monolith` e `build-microservices` (matrix)
- `release.yml` atualizado para taggear imagens de todos os serviços
- `fail-fast: false` em todos os jobs com matrix
- `dependabot.yml` completo com todos os serviços (gradle + docker)

---

## Estrutura dos workflows
```
ci.yml
├── test-monolith              ← build + testes + SonarCloud + Codecov
└── test-microservices         ← matrix: 6 serviços em paralelo
    ├── config-server
    ├── eureka-server
    ├── gateway
    ├── auth-service
    ├── catalog-service
    └── loan-service

docker.yml
├── build-monolith             ← imagem library-api
└── build-microservices        ← matrix: 6 imagens
    ├── library-config-server
    ├── library-eureka-server
    ├── library-gateway
    ├── library-auth-service
    ├── library-catalog-service
    └── library-loan-service

release.yml
├── release-monolith           ← tag library-api:vX.Y.Z
└── release-microservices      ← matrix: tag todos os serviços
```

---

## Decisões e Tradeoffs

### Matrix strategy com fail-fast: false

**Decisão:** `fail-fast: false` em todos os jobs com matrix.

Serviços são independentes. Se o `catalog-service` tem um teste quebrado, não faz sentido cancelar o job do `loan-service` — são bases de código separadas. O `fail-fast: true` (default) faria exatamente isso.

---

### JAR buildado no CI antes do Docker

O `Dockerfile` dos microservices usa:
```dockerfile
COPY build/libs/*.jar app.jar
```

Isso significa que o JAR precisa existir antes do `docker build`. O monolito resolve isso com multi-stage build (Gradle dentro do container). Os microservices resolvem no CI:
```yaml
- name: Build JAR
  working-directory: ${{ matrix.service }}
  run: ./gradlew bootJar --quiet

- name: Build and push
  uses: docker/build-push-action
  with:
    context: ${{ matrix.service }}
```

**Tradeoff:** o Docker image dos microservices não pode ser buildado localmente com `docker build` sem antes rodar `./gradlew bootJar`. O monolito ainda suporta `docker build` standalone via multi-stage.

**Alternativa não adotada:** adicionar multi-stage build em cada Dockerfile dos microservices. Rejeitado — tornaria cada Dockerfile mais complexo e o build no CI mais lento (Gradle rodaria dentro do container sem cache).

---

### Nomenclatura das imagens Docker

| Serviço | Imagem DockerHub |
|---|---|
| monolito | `{user}/library-api` |
| config-server | `{user}/library-config-server` |
| eureka-server | `{user}/library-eureka-server` |
| gateway | `{user}/library-gateway` |
| auth-service | `{user}/library-auth-service` |
| catalog-service | `{user}/library-catalog-service` |
| loan-service | `{user}/library-loan-service` |

Prefixo `library-` em todos para agrupar no DockerHub.

---

### Dependabot completo

Cada serviço tem entrada própria para `gradle` e `docker` — PRs automáticos de atualização de dependências por serviço, sem acumular tudo em um único PR gigante.

---

## Arquivos criados/modificados

| Arquivo | Ação |
|---|---|
| `.github/workflows/ci.yml` | Atualizado — matrix para microservices |
| `.github/workflows/docker.yml` | Atualizado — matrix para microservices |
| `.github/workflows/release.yml` | Atualizado — matrix para microservices |
| `.github/dependabot.yml` | Atualizado — todos os serviços |

---

## Próxima etapa

**Etapa 11 — Merge e Release v2.0.0**

- Abrir PR `microservices` → `main`
- Validar CI completo
- Merge na `main`
- Tag `v2.0.0`
- Atualizar README.md com status final