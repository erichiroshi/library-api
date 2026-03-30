# Etapa 2 & 3 — Config Server + Config Repo

**Branch:** `microservices`  
**Commit:** `feat(config-server): adicionar Config Server com configurações centralizadas`  
**Status:** ✅ Concluída

---

## O que foi feito

- Criado `config-server/` com Spring Cloud Config Server 2025.1.1
- Criado `config-repo/` com YAMLs para todos os serviços
- Config Server subindo na porta `8888`
- Verificado via curl — merging de configurações funcionando
- `config-server` incluído no `settings.gradle` raiz
- `dependabot.yml` atualizado com entrada do `config-server`
- Adicionado `spring-boot-starter-actuator` explícito no `build.gradle`

---

## Estrutura criada
```
config-server/
├── src/main/java/com/example/configserver/
│   └── ConfigServerApplication.java     ← @EnableConfigServer
├── src/main/resources/
│   └── application.yml                  ← porta 8888, perfil native
├── build.gradle
└── settings.gradle

config-repo/
├── application.yml        ← compartilhado por todos os serviços
├── auth-service.yml       ← datasource auth + JWT + Flyway
├── catalog-service.yml    ← datasource catalog + Redis + S3 + Flyway
├── loan-service.yml       ← datasource lending + Feign + Resilience4j + Flyway
├── gateway.yml            ← rotas + JWT secret
└── eureka-server.yml      ← porta 8761 + standalone config
```

---

## Decisões e Tradeoffs

### Perfil `native` vs Git

**Decisão:** perfil `native` — lê YAMLs diretamente do filesystem.

| | Native | Git |
|---|---|---|
| Setup | Zero — aponta para pasta local | Requer repositório Git separado |
| Produção | Não recomendado | Padrão de mercado |
| Histórico de mudanças | Não tem | Git log completo |
| Rollback de config | Manual | `git revert` |
| Portfólio local | Suficiente | Complexidade desnecessária |

**Por que native:** elimina dependência externa para rodar o projeto localmente. Em produção real, trocar para Git requer apenas mudar o perfil e apontar `spring.cloud.config.server.git.uri` — o resto do sistema não muda.

---

### config-repo/application.yml — configurações compartilhadas

O `application.yml` no `config-repo/` é automaticamente merged em todas as respostas do Config Server. Configurações que todos os serviços precisam ficam aqui:

- URL do Eureka (`eureka.client.service-url.defaultZone`)
- `eureka.instance.prefer-ip-address: true`
- Management endpoints (`health`, `info`, `metrics`, `prometheus`)
- `health.show-details: when-authorized`
- Logging level padrão (`root: INFO`)

Cada serviço só declara o que é específico seu — sem repetição.

---

### Por que não existe config-server.yml no config-repo/

O Config Server não é cliente de si mesmo — ele **serve** o `config-repo/`, não busca configuração nele. Toda a configuração do Config Server está em `config-server/src/main/resources/application.yml`. Os outros serviços têm YAML no `config-repo/` porque são clientes do Config Server.

| Serviço | Papel | Configuração fica em |
|---|---|---|
| `config-server` | Servidor | `config-server/src/main/resources/application.yml` |
| `eureka-server` | Cliente | `config-repo/eureka-server.yml` |
| `gateway` | Cliente | `config-repo/gateway.yml` |
| `auth-service` | Cliente | `config-repo/auth-service.yml` |
| `catalog-service` | Cliente | `config-repo/catalog-service.yml` |
| `loan-service` | Cliente | `config-repo/loan-service.yml` |

---

### Spring Cloud 2025.1.1

Spring Boot 4.0.3 requer Spring Cloud 2025.1.x (Oakwood). A versão 2025.0.0 é incompatível com Boot 4.0.1+. Verificado antes de gerar o `build.gradle`.

---

### build.gradle standalone

Cada serviço declara versões de plugins, `repositories { mavenCentral() }` e `spring-boot-starter-actuator` explicitamente. Isso permite rodar cada serviço standalone sem depender do `build.gradle` raiz.

**Regra estabelecida:** todo `build.gradle` de serviço declara:
- Plugins com versão explícita
- `repositories { mavenCentral() }`
- `spring-boot-starter-actuator` explícito
- `--enable-preview` nas tasks de compilação e teste

---

## Verificação
```bash
# Health check
curl http://localhost:8888/actuator/health

# Configuração do auth-service (específica + compartilhada merged)
curl http://localhost:8888/auth-service/default
```

Resposta do `auth-service/default` retornou dois `propertySources`:
1. `file:../config-repo/auth-service.yml` — configurações específicas
2. `file:../config-repo/application.yml` — configurações compartilhadas

Merge automático funcionando corretamente.

---

## Problema encontrado e resolvido

**Problema:** `repositories` não declarado no `build.gradle` do `config-server` causou falha na resolução de dependências ao rodar standalone.

**Causa:** o `build.gradle` raiz declara `mavenCentral()` no bloco `subprojects {}`, mas esse bloco só é aplicado quando o build é executado a partir da raiz do monorepo. Rodando `gradlew bootRun` dentro de `config-server/`, o arquivo raiz não é lido.

**Solução:** declarar `repositories { mavenCentral() }` em cada `build.gradle` de serviço — regra estabelecida para todos os serviços da Fase 3.

---

## Arquivos criados/modificados

| Arquivo | Ação |
|---|---|
| `config-server/build.gradle` | Criado — plugins + actuator explícito |
| `config-server/settings.gradle` | Criado |
| `config-server/src/main/java/.../ConfigServerApplication.java` | Criado — @EnableConfigServer |
| `config-server/src/main/resources/application.yml` | Criado — porta 8888, perfil native |
| `config-repo/application.yml` | Criado — configurações compartilhadas |
| `config-repo/auth-service.yml` | Criado |
| `config-repo/catalog-service.yml` | Criado |
| `config-repo/loan-service.yml` | Criado |
| `config-repo/gateway.yml` | Criado |
| `config-repo/eureka-server.yml` | Criado |
| `settings.gradle` (raiz) | Modificado — `include 'config-server'` |
| `.github/dependabot.yml` | Modificado — entrada do `config-server` |

---

## Próxima etapa

**Etapa 4 — Eureka Server**

- Criar projeto Spring Boot em `eureka-server/` via Spring Initializr
- Configurar como standalone (`register-with-eureka: false`, `fetch-registry: false`)
- Porta fixa `8761`
- Buscar configuração do Config Server via `spring.config.import`
- Fallback local para rodar sem Config Server