# Etapa 9 — Docker Compose Final + Observabilidade

**Branch:** `microservices`  
**Commit:** `chore(docker): adicionar observabilidade e finalizar docker-compose.yml`  
**Status:** ✅ Concluída

---

## O que foi feito

- `docker-compose.yml` completo com 10 serviços orquestrados
- Prometheus `v3.2.1`, Grafana `11.5.2`, Zipkin `3.5.0` adicionados
- Management port fixa `8090` para actuator em todos os serviços (prod)
- Ports de management dev: `auth:8091`, `catalog:8092`, `loan:8093`
- `prometheus.yml` (prod) e `prometheus-dev.yml` (dev) criados
- Observabilidade adicionada em todos os serviços: `micrometer-registry-prometheus`, `spring-boot-starter-zipkin`, `opentelemetry-exporter-zipkin`
- Zipkin e tracing configurados no `config-repo/application.yml`
- W3C Trace Context — traceId propagado em todos os logs

---

## Ordem de subida
```
1. postgres + redis                   infraestrutura
2. config-server      (8888)          configurações
3. eureka-server      (8761)          descoberta
4. gateway            (8080)          entrada única
5. auth-service       (aleatória)     autenticação
6. catalog-service    (aleatória)     catálogo
7. loan-service       (aleatória)     empréstimos
8. prometheus         (9090)          métricas
9. grafana            (3000)          dashboards
10. zipkin            (9411)          tracing
```

---

## Decisões e Tradeoffs

### Management port fixa (8090)

**Problema:** serviços com `port: 0` recebem porta aleatória em runtime. O Docker não consegue fazer healthcheck em porta desconhecida.

**Solução:** `management.server.port: 8090` no `config-repo/application.yml` — o actuator fica sempre em `8090`, independente da porta do servidor.

**Benefício adicional:** o Prometheus scrape usa a mesma porta fixa — sem necessidade de service discovery para métricas.

| Serviço | Porta servidor | Porta actuator prod | Porta actuator dev |
|---|---|---|---|
| gateway | 8080 (fixa) | 8080 | 8080 |
| auth-service | aleatória | 8090 | 8091 |
| catalog-service | aleatória | 8090 | 8092 |
| loan-service | aleatória | 8090 | 8093 |

Em dev as portas são diferentes por serviço para evitar conflito — todos sobem no mesmo host local.

---

### Healthcheck do Prometheus

A imagem `prom/prometheus` não tem `curl`. Solução com `wget`:
```yaml
test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:9090/-/healthy"]
```

---

### Grafana sem healthcheck

A imagem do Grafana não tem `curl` nem `wget` disponíveis de forma confiável. O `depends_on: prometheus` garante que o Prometheus está saudável antes do Grafana subir.

---

### prometheus-dev.yml separado

Em prod, os serviços são referenciados pelo nome do container Docker (`auth-service:8090`). Em dev, sobem localmente com portas específicas. Dois arquivos evitam conflito de configuração entre ambientes.

---

### Observabilidade em todos os serviços
```groovy
implementation 'io.micrometer:micrometer-registry-prometheus'
implementation 'org.springframework.boot:spring-boot-starter-zipkin'
implementation 'io.opentelemetry:opentelemetry-exporter-zipkin'
```

Mesmo stack do monolito — consistência entre monolito e microservices facilita comparação de traces no Zipkin.

TraceId propagado nos logs via padrão:
```
%d{yyyy-MM-dd HH:mm:ss.SSS} %5level [%X{traceId:-},%X{spanId:-}] [%thread] ...
```

W3C Trace Context — compatível com qualquer ferramenta de APM.

---

## Arquivos criados/modificados

| Arquivo | Ação |
|---|---|
| `docker-compose.yml` | Finalizado — 10 serviços com healthchecks e ordem correta |
| `monitoring/prometheus/prometheus.yml` | Criado — scrape prod (porta 8090) |
| `monitoring/prometheus/prometheus-dev.yml` | Criado — scrape dev (portas 8091-8093) |
| `monitoring/grafana/provisioning/datasources/datasource.yml` | Criado |
| `monitoring/grafana/provisioning/dashboards/dashboard.yml` | Criado |
| `config-repo/application.yml` | Atualizado — management.server.port: 8090, zipkin, tracing |
| `config-repo/application-dev.yml` | Criado — configs dev compartilhadas |
| `config-repo/auth-service-dev.yml` | Atualizado — management.server.port: 8091 |
| `config-repo/catalog-service-dev.yml` | Atualizado — management.server.port: 8092 |
| `config-repo/loan-service-dev.yml` | Atualizado — management.server.port: 8093 |
| Todos os `build.gradle` dos serviços | Atualizado — Prometheus + Zipkin + OpenTelemetry |

---

## Verificação
```bash
# Subir tudo
docker compose up -d

# Verificar serviços
docker compose ps

# Prometheus — targets ativos
http://localhost:9090/targets

# Grafana
http://localhost:3000  (admin/admin)

# Zipkin — traces
http://localhost:9411

# Eureka — serviços registrados
http://localhost:8761
```

---

## Próxima etapa

**Etapa 10 — CI/CD atualizado**

- Atualizar `ci.yml` para rodar testes em todos os subprojetos
- Matrix strategy para os serviços da Fase 3
- Atualizar `docker.yml` para buildar imagem de cada serviço