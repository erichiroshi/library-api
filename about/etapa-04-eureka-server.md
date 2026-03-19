# Etapa 4 — Eureka Server

**Branch:** `microservices`  
**Commit:** `feat(eureka-server): adicionar Eureka Server com service discovery`  
**Status:** ✅ Concluída

---

## O que foi feito

- Gerado `eureka-server/` via Spring Initializr (Spring Boot 4.0.4)
- Configurado como standalone — não se registra em si mesmo
- Porta fixa `8761`
- Configuração buscada do Config Server via `spring.config.import`
- Fallback local para rodar sem Config Server
- Dashboard verificado em `http://localhost:8761`
- `eureka-server` incluído no `settings.gradle` raiz
- `dependabot.yml` atualizado

---

## Estrutura criada
```
eureka-server/
├── src/main/java/com/example/eurekaserver/
│   └── EurekaServerApplication.java     ← @EnableEurekaServer
├── src/main/resources/
│   └── application.yml                  ← config.import + fallback local
├── build.gradle
├── settings.gradle
├── gradlew / gradlew.bat                ← wrapper próprio (Initializr)
└── gradle/wrapper/
```

---

## Decisões e Tradeoffs

### Eureka vs alternativas

**Decisão:** Netflix Eureka.

| | Eureka | Consul | Kubernetes |
|---|---|---|---|
| Integração Spring Cloud | Nativa | Boa | Nativa (diferente) |
| Infraestrutura extra | Nenhuma | Consul agent | Cluster K8s |
| Complexidade local | Baixa | Média | Alta |
| Produção real | Sim | Sim | Padrão atual |
| Path para K8s | Troca só o client | Troca só o client | Já está lá |

**Por que Eureka:** zero infraestrutura extra, integração nativa com Spring Cloud Load Balancer e OpenFeign, e path claro para Kubernetes — quando migrar, basta remover a dependência do Eureka client e usar o service discovery do K8s. O código dos serviços não muda.

---

### Porta fixa vs aleatória

**Decisão:** porta fixa `8761`.

O Eureka é o ponto central de descoberta — todos os serviços precisam saber onde ele está para se registrar. Porta aleatória quebraria o bootstrap dos outros serviços. O mesmo vale para o Gateway (`8080`) e o Config Server (`8888`). Apenas os serviços de negócio (`auth`, `catalog`, `loan`) usam porta `0`.

---

### spring.config.import + fallback local

**Problema encontrado:** o Eureka não buscou configuração do Config Server na primeira tentativa — subiu na porta `8080` em vez de `8761`.

**Causa:** `spring.config.import` requer a dependência `spring-cloud-starter-config` no classpath. Sem ela, o import é ignorado silenciosamente.

**Solução:** adicionar `spring-cloud-starter-config` ao `build.gradle` + manter fallback local no `application.yml` com as configurações essenciais (`server.port`, `register-with-eureka: false`, `fetch-registry: false`).

**Comportamento resultante:**
- Com Config Server no ar: configurações do `config-repo/eureka-server.yml` sobrescrevem o fallback
- Sem Config Server: `optional:configserver:` ignora a falha e usa o fallback local

---

### Ajuste no build.gradle gerado pelo Initializr

O Initializr gerou Spring Cloud `2025.1.0`. Atualizado para `2025.1.1` por consistência com o `config-server`. Adicionado `--enable-preview` nas tasks de compilação e teste (padrão do projeto).

---

## Verificação
```
http://localhost:8761
```

Dashboard Eureka exibindo "No instances currently registered with Eureka" — correto, nenhum serviço registrado ainda.

---

## Ordem de subida atual
```
1. Config Server  (porta 8888)
2. Eureka Server  (porta 8761)
```

---

## Arquivos criados/modificados

| Arquivo | Ação |
|---|---|
| `eureka-server/` | Criado via Spring Initializr |
| `eureka-server/build.gradle` | Ajustado — Spring Cloud 2025.1.1 + config client + enable-preview |
| `eureka-server/src/main/resources/application.yml` | Criado — substitui application.properties |
| `eureka-server/src/main/java/.../EurekaServerApplication.java` | Ajustado — @EnableEurekaServer adicionado |
| `settings.gradle` (raiz) | Modificado — `include 'eureka-server'` |
| `.github/dependabot.yml` | Modificado — entrada do `eureka-server` |

---

## Próxima etapa

**Etapa 5 — Gateway**

- Criar projeto Spring Boot em `gateway/`
- Spring Cloud Gateway com JWT centralizado
- Rotas para todos os serviços
- Filtro JWT — valida token e propaga `X-User-Id` e `X-User-Roles`
- Porta fixa `8080`