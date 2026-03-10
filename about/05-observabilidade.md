# Observabilidade — OpenTelemetry, Zipkin, Prometheus e Grafana

## O que este documento cobre

Como a observabilidade foi implementada no projeto: o que é observabilidade e por que importa, tracing distribuído com OpenTelemetry e Zipkin, métricas com Micrometer e Prometheus, dashboards com Grafana, alertas, métricas customizadas de negócio, e como tudo isso se torna ainda mais valioso na transição para microservices.

---

## O que é Observabilidade

Observabilidade é a capacidade de entender o estado interno de um sistema a partir das suas saídas externas. Em sistemas de software, isso se traduz em três pilares:

```
┌─────────────────────────────────────────────────────────────┐
│                     Os 3 Pilares                            │
│                                                             │
│  LOGS          MÉTRICAS         TRACES                      │
│  "o que        "como está       "o que aconteceu            │
│  aconteceu"    indo agora"      nessa requisição"           │
│                                                             │
│  Texto         Números          Grafo de chamadas           │
│  Eventos       Séries           com tempo de cada etapa     │
│  Erros         temporais                                    │
└─────────────────────────────────────────────────────────────┘
```

Sem observabilidade, quando algo dá errado em produção, a investigação é manual e lenta — verificar logs linha por linha, sem contexto de qual requisição causou o problema. Com observabilidade, é possível ir diretamente ao trace da requisição que falhou, ver exatamente onde demorou ou errou, e correlacionar com métricas do sistema naquele momento.

---

## A Stack de Observabilidade

```
Aplicação
    │
    ├── Logs ───────────────── Logback (com traceId nos logs)
    │
    ├── Traces ─────────────── OpenTelemetry → Zipkin
    │
    └── Métricas ───────────── Micrometer → Prometheus → Grafana
```

Cada ferramenta tem uma responsabilidade específica. Nenhuma faz tudo — elas se complementam.

---

## Micrometer: A Camada de Abstração de Métricas

O Micrometer é para métricas o que o SLF4J é para logs — uma fachada que abstrai o sistema de métricas subjacente. O código da aplicação usa as APIs do Micrometer, e o Micrometer envia os dados para o backend configurado (Prometheus, no caso).

Isso significa que se no futuro o Prometheus for substituído por Datadog, New Relic ou outra ferramenta, o código da aplicação não muda — só a configuração do Micrometer.

### Métricas automáticas

O Spring Boot Actuator + Micrometer já expõe dezenas de métricas automaticamente:

```
# JVM
jvm_memory_used_bytes
jvm_gc_pause_seconds
jvm_threads_live_threads

# HTTP
http_server_requests_seconds_count
http_server_requests_seconds_sum

# HikariCP (pool de conexões)
hikaricp_connections_active
hikaricp_connections_pending

# Cache Redis
cache_gets_total{result="hit"}
cache_gets_total{result="miss"}
```

Todas essas métricas ficam disponíveis no endpoint `/actuator/prometheus` no formato que o Prometheus entende.

---

## Prometheus: Coleta de Métricas

O Prometheus é um sistema de monitoramento que funciona por **pull** — ele consulta periodicamente o endpoint de métricas da aplicação e armazena os valores em sua base de dados de séries temporais.

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'library-api'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8080']
    scrape_interval: 10s   # coleta a cada 10 segundos
```

### Pull vs Push

O modelo pull (Prometheus consulta a aplicação) tem vantagens:
- A aplicação não precisa saber onde está o Prometheus
- Se o Prometheus reiniciar, ele retoma a coleta sem mudança na aplicação
- Fácil de verificar se a aplicação está acessível (se o scrape falha, há um alerta)

A desvantagem é que a aplicação precisa ter um endpoint exposto. Em ambientes muito restritos de rede, o modelo push (aplicação envia para o servidor) pode ser mais adequado.

---

## Grafana: Visualização

O Grafana consome os dados do Prometheus e os exibe em dashboards. No projeto, os dashboards são **provisionados automaticamente** — não precisam ser criados manualmente na interface.

```yaml
# provisioning/datasources/datasource.yml
datasources:
  - name: Prometheus
    type: prometheus
    url: http://prometheus:9090
    isDefault: true
```

```yaml
# provisioning/dashboards/dashboard.yml
providers:
  - name: 'library'
    type: file
    options:
      path: /etc/grafana/provisioning/dashboards
```

O JSON do dashboard fica versionado no repositório. Quando o Grafana sobe, importa o dashboard automaticamente. Isso é **infrastructure as code** — o estado do Grafana é reproduzível a partir do repositório, sem configuração manual.

### Painéis do Dashboard

```
┌─────────────────┬───────────────────────────────────┐
│  Total Books    │   Requests per Second (RPS)       │
│  [stat]         │   [timeseries]                    │
├─────────────────┴──────────────────────────────────-┤
│  Requests by Endpoint    │  5xx Errors per Second   │
│  [timeseries]            │  [timeseries]            │
├──────────────────────────┴──────────────────────────┤
│  Average Response Time   │  Error Rate (%)          │
│  [timeseries]            │  [stat]                  │
└──────────────────────────┴──────────────────────────┘
```

As queries PromQL por trás dos painéis:

```promql
# RPS
rate(http_server_requests_seconds_count{uri!="/actuator/prometheus"}[1m])

# Tempo médio de resposta em ms
rate(http_server_requests_seconds_sum[1m]) 
  / rate(http_server_requests_seconds_count[1m]) * 1000

# Taxa de erro
(rate(http_server_requests_seconds_count{status=~"5.."}[1m])
  / rate(http_server_requests_seconds_count[1m])) * 100
```

O filtro `uri!="/actuator/prometheus"` exclui as próprias requisições do Prometheus ao endpoint de métricas — sem isso, o scrape do Prometheus inflacionaria artificialmente as métricas de RPS.

---

## Alertas no Prometheus

```yaml
# alerts.yml
groups:
  - name: library-api
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High error rate detected"

      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes / jvm_memory_max_bytes > 0.9
        for: 5m
        labels:
          severity: critical
```

Os alertas definem condições que, se mantidas por um período (`for: 5m`), disparam uma notificação. O `for` evita falsos positivos — um pico momentâneo de 5 segundos não dispara alerta, mas uma condição sustentada por 5 minutos sim.

**HighErrorRate**: mais de 0.05 erros 5xx por segundo em média nos últimos 5 minutos. Taxa de 5% de erros é um limiar razoável para warning em muitos sistemas.

**HighMemoryUsage**: heap JVM acima de 90% da memória máxima por 5 minutos. Indica risco iminente de `OutOfMemoryError`.

---

## Métricas Customizadas de Negócio

Além das métricas automáticas de infraestrutura, o projeto implementa uma métrica de negócio: contador de livros criados.

```java
// BookMetricsConfig.java
@Bean
Counter bookCreatedCounter(MeterRegistry registry) {
    return Counter.builder("library.books.created")
            .description("Quantidade de livros criados")
            .register(registry);
}
```

```java
// BookService.java
@CacheEvict(value = "books", allEntries = true)
@Transactional
public BookResponseDTO create(BookCreateDTO dto) {
    // ... lógica de criação ...
    Book saved = repository.save(book);
    bookCreatedCounter.increment();  // ← incrementa o counter
    return mapper.toDTO(saved);
}
```

### Por que métricas de negócio importam

Métricas de infraestrutura (CPU, memória, RPS) dizem que o sistema está funcionando. Métricas de negócio dizem que o sistema está sendo útil.

Um sistema pode ter CPU baixa e zero erros, mas se nenhum livro está sendo criado quando deveria estar, algo está errado no negócio. Métricas de negócio permitem detectar anomalias que métricas técnicas não capturam.

No Grafana, a query seria:

```promql
library_books_created_total
# ou para ver a taxa de criação
rate(library_books_created_total[5m])
```

---

## OpenTelemetry: Tracing Distribuído

OpenTelemetry é um padrão aberto para instrumentação de aplicações. Ele define como coletar traces (rastros de execução) de forma vendor-neutral — o mesmo código de instrumentação funciona com Zipkin, Jaeger, Datadog, etc.

### O que é um trace

Um trace representa o caminho completo de uma requisição pelo sistema:

```
Trace: GET /api/v1/books/1
│
├── [span] JwtAuthenticationFilter (2ms)
│
├── [span] BookController.findById (1850ms total)
│     │
│     ├── [span] ArtificialDelayService.delay (2000ms) ← dev
│     │
│     └── [span] BookRepository.findById (5ms)
│               │
│               └── [span] PostgreSQL query (3ms)
```

Cada span tem: nome, tempo de início, duração, status (ok/erro) e atributos. O trace mostra exatamente onde o tempo foi gasto.

### W3C Trace Context

```yaml
management:
  tracing:
    propagation:
      type: w3c
```

O W3C Trace Context é um padrão aberto (recomendação do W3C) que define como o traceId e o spanId são propagados entre serviços via headers HTTP:

```
traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
             └─ version ─┘└──── traceId (128 bits) ────┘└─ parentSpanId ┘└─ flags ┘
```

### Por que W3C e não B3 (formato Zipkin)?

O B3 é o formato proprietário do Zipkin/Brave. O W3C Trace Context é um padrão aberto com amplo suporte. A escolha do W3C garante que o projeto pode trocar de backend de tracing (de Zipkin para Jaeger, por exemplo) sem mudar a propagação de contexto entre serviços. Vendor lock-in reduzido.

### traceId nos logs

O traceId aparece automaticamente em cada linha de log, correlacionando logs com traces:

```xml
<!-- logback-spring.xml -->
<property name="LOG_PATTERN"
    value="%d{yyyy-MM-dd HH:mm:ss.SSS} %5level [${appName},%X{traceId:- },%X{spanId:- }] [%thread] %-40logger : %msg%n"/>
```

Resultado nos logs:

```
2026-03-09 14:23:01.123  INFO [library,4bf92f3577b34da6a3ce929d0,00f067aa0ba902b7] [http-nio-8080-exec-1] BookService : Searching book with id=1
2026-03-09 14:23:03.130  INFO [library,4bf92f3577b34da6a3ce929d0,00f067aa0ba902b7] [http-nio-8080-exec-1] BookService : Book found: Clean Code
```

As duas linhas têm o mesmo `traceId` — é possível filtrar todos os logs de uma requisição específica pelo traceId.

### Configuração de sampling

```yaml
# dev — 100% das requisições rastreadas
management:
  tracing:
    sampling:
      probability: 1.0

# prod — 10% das requisições rastreadas
management:
  tracing:
    sampling:
      probability: 0.1
```

Em produção, rastrear 100% das requisições seria custoso em memória e I/O. 10% é um compromisso comum — suficiente para análise estatística e debugging, sem overhead excessivo. O valor é configurável via variável de ambiente `TRACING_SAMPLING_PROBABILITY`.

---

## Zipkin: Backend de Tracing

O Zipkin é a interface de visualização e armazenamento de traces. Recebe os spans enviados pelo OpenTelemetry e os exibe como grafos de dependência e timelines.

```yaml
management:
  tracing:
    export:
      zipkin:
        endpoint: http://zipkin:9411/api/v2/spans  # prod/docker
        # endpoint: http://localhost:9411/...       # dev local
```

No Zipkin é possível:
- Buscar traces por serviço, endpoint, duração, status
- Ver o breakdown de tempo por span
- Identificar qual parte da cadeia está lenta
- Ver erros e exceções dentro de cada span

### Valor real em produção

Um exemplo concreto: usuário reporta que `GET /books/1` está lento apenas às 14h. No Zipkin, filtra traces desse endpoint nesse horário e encontra que o `BookRepository.findById` está levando 800ms em vez de 5ms. Isso aponta para um problema no PostgreSQL (slow query, lock contention) naquele período — algo que os logs sozinhos não revelariam com clareza.

---

## Spring Actuator: A Base de Tudo

Todas as integrações de observabilidade dependem do Spring Actuator, que expõe os endpoints:

```
/actuator/health      → status da aplicação e dependências
/actuator/prometheus  → métricas no formato Prometheus
/actuator/info        → informações da aplicação
/actuator/metrics     → métricas no formato JSON
```

### Exposição por profile

```yaml
# dev — tudo exposto
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,traces

# prod — apenas o necessário
management:
  endpoints:
    web:
      exposure:
        include: health,prometheus
```

Em produção, expor todos os endpoints do Actuator é um risco de segurança — alguns expõem informações sensíveis sobre a aplicação (env vars, configurações, heap dumps). Expor apenas `health` e `prometheus` é o mínimo necessário para monitoramento.

---

## Fase 3: Observabilidade em Microservices

É aqui que a observabilidade se torna essencial, não opcional. Com 7 serviços independentes, rastrear um problema sem traces seria praticamente impossível.

### O traceId percorre todos os serviços

```
Requisição: POST /api/v1/loans
│
├── Gateway (traceId: abc123)
│     └── propaga traceparent header
│
├── loan-service (traceId: abc123 — mesmo trace)
│     └── chama catalog-service via Feign
│           └── propaga traceparent header
│
└── catalog-service (traceId: abc123 — mesmo trace)
      └── decrementa cópias
```

No Zipkin, o trace `abc123` mostra spans de todos os três serviços em uma única visualização — Gateway, loan-service e catalog-service. Se o catalog-service estiver lento, fica imediatamente visível qual serviço é o gargalo.

Sem traces distribuídos, isso exigiria correlacionar logs manualmente de três serviços diferentes pelo timestamp — trabalhoso e propenso a erro.

### Métricas por serviço

Cada microservice expõe seu próprio `/actuator/prometheus`. O Prometheus é configurado com múltiplos jobs de scrape:

```yaml
scrape_configs:
  - job_name: 'auth-service'
    static_configs:
      - targets: ['auth-service:8081']  # ou via Eureka

  - job_name: 'catalog-service'
    static_configs:
      - targets: ['catalog-service:8082']

  - job_name: 'loan-service'
    static_configs:
      - targets: ['loan-service:8083']
```

No Grafana, é possível criar dashboards comparativos — latência do `catalog-service` vs `loan-service`, taxa de erro por serviço, etc.

### Service discovery e scraping dinâmico

Com Eureka, o Prometheus pode usar service discovery para descobrir automaticamente novos serviços registrados — sem precisar atualizar o `prometheus.yml` manualmente a cada novo serviço ou instância.

---

## Resumo das Decisões de Observabilidade

| Decisão | Alternativa | Por que esta |
|---|---|---|
| Micrometer como abstração | Prometheus SDK direto | Troca de backend sem mudar código |
| W3C Trace Context | B3 (formato Zipkin nativo) | Padrão aberto, sem vendor lock-in |
| Zipkin | Jaeger | Mais simples de rodar localmente; mesma API OpenTelemetry |
| Dashboards como código (JSON no repo) | Configuração manual no Grafana | Reproduzível, versionado, sem perda de configuração |
| 10% sampling em prod | 100% sampling | Custo proporcional ao tráfego; suficiente para análise estatística |
| Métricas de negócio customizadas | Só métricas de infraestrutura | Detecta anomalias funcionais que métricas técnicas não capturam |
| Alertas em `alerts.yml` | Alertas no Grafana | Versionado no repositório; Prometheus independente do Grafana para alertar |
| Actuator restrito em prod | Todos endpoints expostos | Segurança — endpoints sensíveis não expostos em produção |
| traceId nos logs | Logs sem correlação | Filtra todos os logs de uma requisição pelo traceId |
