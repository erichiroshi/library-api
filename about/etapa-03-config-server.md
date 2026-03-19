# Etapa 2 — Config Server

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

---

## Estrutura criada
```
config-server/
├── src/main/java/com/example/configserver/
│   └── ConfigServerApplication.java
├── src/main/resources/
│   └── application.yml
├── build.gradle
└── settings.gradle

config-repo/
├── application.yml        ← compartilhado por todos
├── auth-service.yml
├── catalog-service.yml
├── loan-service.yml
├── gateway.yml
└── eureka-server.yml
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

### Spring Cloud 2025.1.1

Spring Boot 4.0.3 requer Spring Cloud 2025.1.x (Oakwood). A versão 2025.0.0 é incompatível com Boot 4.0.1+. Verificado antes de gerar o `build.gradle`.

---

### `build.gradle` standalone

Cada serviço declara versões de plugins e `repositories { mavenCentral() }` localmente. Isso permite rodar cada serviço com `gradlew bootRun` standalone, sem depender do `build.gradle` raiz.

O `build.gradle` raiz continua com `apply false` — serve para builds multi-project quando todos os serviços estiverem prontos.

**Regra estabelecida:** todo `build.gradle` de serviço declara plugins com versão + `repositories` explicitamente.

---

### Configurações compartilhadas via `application.yml`

O `config-repo/application.yml` é automaticamente merged em todas as respostas do Config Server. Configurações que todos os serviços precisam ficam aqui:

- URL do Eureka
- Management endpoints
- Logging level padrão

Cada serviço só declara o que é específico seu.

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

## Regras estabelecidas nesta etapa

- Todo `build.gradle` de serviço declara versões de plugins e `repositories` explicitamente
- `dependabot.yml` atualizado a cada novo serviço criado (regra da Etapa 1 aplicada)

---

## Arquivos criados/modificados

| Arquivo | Ação |
|---|---|
| `config-server/build.gradle` | Criado |
| `config-server/settings.gradle` | Criado |
| `config-server/src/main/java/.../ConfigServerApplication.java` | Criado |
| `config-server/src/main/resources/application.yml` | Criado |
| `config-repo/application.yml` | Criado |
| `config-repo/auth-service.yml` | Criado |
| `config-repo/catalog-service.yml` | Criado |
| `config-repo/loan-service.yml` | Criado |
| `config-repo/gateway.yml` | Criado |
| `config-repo/eureka-server.yml` | Criado |
| `settings.gradle` (raiz) | Modificado — `include 'config-server'` |
| `.github/dependabot.yml` | Modificado — entrada do `config-server` |

---

## Próxima etapa

**Etapa 3 — Eureka Server**

- Criar projeto Spring Boot em `eureka-server/`
- Configurar como standalone (não se registra em si mesmo)
- Porta fixa `8761`
- Buscar configuração do Config Server via `bootstrap.yml`