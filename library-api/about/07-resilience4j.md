# Resilience4j — Circuit Breaker e Retry

## O que este documento cobre

Como a resiliência foi implementada no projeto: o que é o Resilience4j, os dois padrões utilizados (Circuit Breaker e Retry), onde cada um foi aplicado e por quê, a diferença de comportamento entre eles, configuração, e os trade-offs de cada decisão.

---

## Por que Resiliência?

Sistemas distribuídos dependem de componentes externos — banco de dados, Redis, AWS S3, outros serviços. Componentes externos falham. A questão não é se vão falhar, mas quando.

Sem proteção, uma falha em cascata funciona assim:

```
S3 lento (latência 30s)
  → threads da aplicação ficam presas esperando
  → pool de threads esgota
  → novas requisições ficam na fila
  → timeout para o cliente
  → aplicação inteira parece estar fora do ar
```

O problema não foi o S3 — foi a aplicação não saber parar de tentar quando deveria.

Resilience4j oferece padrões para lidar com isso: **Circuit Breaker** para proteger contra dependências instáveis, e **Retry** para lidar com falhas transitórias.

---

## Resilience4j no Projeto

```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      s3:
        slidingWindowSize: 5
        failureRateThreshold: 60
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 3
        registerHealthIndicator: true

  retry:
    instances:
      database:
        maxAttempts: 3
        waitDuration: 500ms
        retryExceptions:
          - org.springframework.dao.TransientDataAccessException
          - org.springframework.dao.QueryTimeoutException
```

Dois instances configurados: `s3` para o Circuit Breaker e `database` para o Retry. Estratégias diferentes para problemas diferentes.

---

## Circuit Breaker

### O conceito

O Circuit Breaker é inspirado nos disjuntores elétricos: quando algo dá errado repetidamente, ele "abre" o circuito e para de tentar, protegendo o sistema de desperdício de recursos e cascata de falhas.

```
CLOSED → falhas acumulam → OPEN → timeout → HALF-OPEN → sucesso → CLOSED
                                                        ↘ falha → OPEN
```

### Os três estados

**CLOSED** (circuito fechado — funcionando normalmente):
- Todas as chamadas passam para a dependência
- O Resilience4j monitora a taxa de falhas na janela deslizante
- Se a taxa de falhas ultrapassa o threshold, transiciona para OPEN

**OPEN** (circuito aberto — protegendo):
- Chamadas são **rejeitadas imediatamente** sem tentar a dependência
- O fallback é chamado diretamente
- Após `waitDurationInOpenState` (30s), transiciona para HALF-OPEN

**HALF-OPEN** (testando recuperação):
- Permite um número limitado de chamadas de teste (`permittedNumberOfCallsInHalfOpenState: 3`)
- Se as chamadas de teste têm sucesso → volta para CLOSED
- Se falham → volta para OPEN

### Configuração do instance `s3`

```yaml
s3:
  slidingWindowSize: 5          # janela de 5 chamadas para calcular taxa de falha
  failureRateThreshold: 60      # abre se 60% das chamadas falharem (3 de 5)
  waitDurationInOpenState: 30s  # fica OPEN por 30s antes de tentar recuperar
  permittedNumberOfCallsInHalfOpenState: 3  # 3 chamadas de teste no HALF-OPEN
```

**`slidingWindowSize: 5`**: janela pequena porque uploads de imagem são operações pouco frequentes. Uma janela de 100 dificilmente seria preenchida — o CB nunca abriria.

**`failureRateThreshold: 60`**: 3 de 5 chamadas falhando já indica problema sério. Threshold menor (30%) seria muito sensível para operações ocasionais.

**`waitDurationInOpenState: 30s`**: 30 segundos é tempo suficiente para uma instabilidade transitória da AWS se resolver, sem deixar o serviço de upload indisponível por muito tempo.

### Onde foi aplicado: S3

```java
@CircuitBreaker(name = "s3", fallbackMethod = "uploadFallback")
public String uploadCover(Long bookId, MultipartFile file) {
    // upload para o S3
}

@CircuitBreaker(name = "s3", fallbackMethod = "deleteFallback")
public void deleteCover(String coverImageUrl) {
    // deleção do S3
}
```

O S3 é a dependência externa mais suscetível a instabilidade do ponto de vista da aplicação — latência de rede, timeouts, erros da AWS. Circuit Breaker aqui evita que falhas no S3 segurem threads enquanto esperam timeouts.

### Por que não Retry no S3?

Upload de arquivo **não é idempotente**. Se a primeira tentativa teve sucesso parcial (o arquivo foi enviado mas a confirmação de resposta foi perdida), fazer retry enviaria o arquivo novamente — criando duplicatas no S3.

Além disso, fazer retry de um arquivo de 5MB com 3 tentativas significa potencialmente enviar 15MB por upload. Para o usuário, a latência seria inaceitável.

Circuit Breaker sem retry é a estratégia correta para uploads.

---

## Retry

### O conceito

Retry é mais simples: tenta novamente após uma falha. Útil para falhas **transitórias** — erros que desaparecem sozinhos em milissegundos a poucos segundos, como um deadlock de banco que se resolve, uma conexão do pool que estava momentaneamente indisponível, ou um timeout esporádico.

A distinção importante: Retry é para falhas passageiras. Circuit Breaker é para dependências com problemas persistentes. Usar Retry numa dependência sistematicamente falhando piora o problema — aumenta a carga na dependência com falência.

### Configuração do instance `database`

```yaml
database:
  maxAttempts: 3         # tenta no máximo 3 vezes
  waitDuration: 500ms    # espera 500ms entre tentativas
  retryExceptions:       # só faz retry nessas exceções específicas
    - org.springframework.dao.TransientDataAccessException
    - org.springframework.dao.QueryTimeoutException
```

**`maxAttempts: 3`**: 3 tentativas com 500ms de espera = até 1.5s de delay no pior caso. Aceitável para uma operação de banco que raramente falha transitoramente.

**`waitDuration: 500ms`**: tempo suficiente para um deadlock se resolver ou uma conexão do pool ser liberada.

**`retryExceptions`**: lista explícita de exceções que justificam retry. `TransientDataAccessException` é a abstração do Spring para erros transitórios de banco (deadlocks, timeouts de conexão). Erros permanentes como `DataIntegrityViolationException` (violação de constraint) **não devem** ser retentados — a segunda tentativa vai falhar da mesma forma.

### Backoff exponencial

Para sistemas de produção, o backoff fixo de 500ms pode ser insuficiente sob alta carga. O ideal seria backoff exponencial com jitter:

```yaml
database:
  maxAttempts: 3
  waitDuration: 200ms
  enableExponentialBackoff: true
  exponentialBackoffMultiplier: 2
  # tentativas: 200ms → 400ms → 800ms
```

O jitter (variação aleatória) evita o "thundering herd" — múltiplas instâncias retentando ao mesmo tempo no mesmo intervalo, sobrecarregando o banco exatamente quando ele está sob pressão. No projeto atual com uma instância, fixed backoff é suficiente.

---

## Interação entre Circuit Breaker e Retry

Quando usados juntos, a ordem de aplicação importa. Com anotações do Spring, a ordem padrão é:

```
Retry(CircuitBreaker(chamada))
```

O Retry é o mais externo: se a chamada falha, ele tenta novamente. Cada tentativa passa pelo Circuit Breaker. Isso significa que falhas de retry contribuem para a taxa de falhas do Circuit Breaker — o CB "vê" todas as tentativas, não apenas a primeira.

Isso é o comportamento correto: se uma operação falha 3 vezes seguidas (3 tentativas de Retry), o CB registra 3 falhas — aproximando-se mais rápido do threshold de abertura.

No projeto, S3 usa apenas CB (sem Retry) e banco usa apenas Retry (sem CB) — não há interação direta entre eles. Mas na Fase 3, com chamadas entre serviços via Feign, a combinação CB + Retry será necessária.

---

## Fallbacks

### Upload (lança exceção controlada)

```java
private String uploadFallback(Long bookId, MultipartFile file, Exception ex) {
    log.warn("S3 unavailable for bookId={}. Circuit breaker activated.", bookId, ex);
    throw new S3UnavailableException("Cover upload temporarily unavailable.");
}
```

Resultado: `503 Service Unavailable` com mensagem clara. O cliente sabe que o serviço está temporariamente indisponível e pode tentar mais tarde.

### Deleção (falha silenciosa com log)

```java
private void deleteFallback(String coverImageUrl, Exception ex) {
    log.error("Failed to delete S3 object: {}. Manual cleanup required.", coverImageUrl, ex);
    // não lança exceção — a deleção do livro no banco prossegue
}
```

Intencionalmente silencioso: a falha no S3 não impede a deleção do livro. O arquivo órfão é um problema menor que pode ser resolvido depois — via job de limpeza ou manualmente.

### Boas práticas de fallback

- O fallback deve ser mais simples que a operação original — nunca deve chamar a mesma dependência problemática
- O tipo de retorno do fallback deve ser compatível com o método protegido
- O último parâmetro do fallback deve ser `Exception` para capturar a causa raiz
- Logar a exceção no fallback é essencial para diagnóstico posterior

---

## Health Indicator

```yaml
resilience4j:
  circuitbreaker:
    instances:
      s3:
        registerHealthIndicator: true
```

Com `registerHealthIndicator: true`, o estado do Circuit Breaker aparece no `/actuator/health`:

```json
{
  "status": "UP",
  "components": {
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "s3": {
          "status": "CIRCUIT_CLOSED",
          "details": {
            "failureRate": "0.0%",
            "slowCallRate": "0.0%",
            "state": "CLOSED"
          }
        }
      }
    }
  }
}
```

Quando o CB está OPEN:

```json
{
  "s3": {
    "status": "CIRCUIT_OPEN",
    "details": {
      "failureRate": "80.0%",
      "state": "OPEN"
    }
  }
}
```

Isso integra diretamente com o Prometheus/Grafana — é possível alertar quando o Circuit Breaker do S3 abre.

---

## Métricas do Resilience4j

O Resilience4j expõe métricas automaticamente via Micrometer:

```
# Circuit Breaker
resilience4j_circuitbreaker_state{name="s3"}
resilience4j_circuitbreaker_failure_rate{name="s3"}
resilience4j_circuitbreaker_calls_total{name="s3", kind="successful"}
resilience4j_circuitbreaker_calls_total{name="s3", kind="failed"}
resilience4j_circuitbreaker_calls_total{name="s3", kind="not_permitted"}  # rejeitadas pelo CB aberto

# Retry
resilience4j_retry_calls_total{name="database", kind="successful_without_retry"}
resilience4j_retry_calls_total{name="database", kind="successful_with_retry"}
resilience4j_retry_calls_total{name="database", kind="failed_with_retry"}
```

`not_permitted` é particularmente útil: indica requisições rejeitadas pelo CB aberto sem nem tentar a dependência. Se esse número aumenta, o CB está ativo e protegendo o sistema.

---

## Fase 3: Resilience4j nos Microservices

Na Fase 3, o Resilience4j ganha protagonismo. Com chamadas entre serviços via **OpenFeign**, cada chamada entre `loan-service` → `catalog-service` é uma dependência externa que pode falhar.

### CB + Retry para chamadas entre serviços

```java
// loan-service chamando catalog-service
@CircuitBreaker(name = "catalog-service", fallbackMethod = "catalogFallback")
@Retry(name = "catalog-service")
CatalogBookDTO getBook(Long bookId);
```

A combinação faz sentido aqui: Retry para falhas transitórias (instância do catalog-service reiniciando), Circuit Breaker para falhas persistentes (catalog-service fora do ar).

### Bulkhead (novo na Fase 3)

Na Fase 3, pode ser adicionado o **Bulkhead** — limita o número de chamadas simultâneas para um serviço:

```yaml
resilience4j:
  bulkhead:
    instances:
      catalog-service:
        maxConcurrentCalls: 20
```

Se o `catalog-service` estiver lento, o Bulkhead garante que no máximo 20 threads do `loan-service` ficam esperando resposta. O restante recebe rejeição imediata em vez de esperar indefinidamente — preservando threads para outras operações.

### Timeout (novo na Fase 3)

```yaml
resilience4j:
  timelimiter:
    instances:
      catalog-service:
        timeoutDuration: 2s
```

Chamadas que excedem 2 segundos são interrompidas. Sem timeout, uma chamada lenta pode segurar uma thread indefinidamente. O timeout força uma falha controlada que o CB e o Retry podem tratar.

A combinação completa para microservices: **Bulkhead → CB → Retry → Timeout**, cada um com seu papel na cadeia de resiliência.

---

## Resumo das Decisões de Resiliência

| Decisão | Alternativa | Por que esta |
|---|---|---|
| Circuit Breaker no S3 | Sem proteção | Evita cascata de falhas e esgotamento de threads em instabilidade do S3 |
| Sem Retry no upload S3 | Retry no upload | Upload não é idempotente; retry de arquivo = latência e risco de duplicata |
| Retry no banco (exceções específicas) | Retry em qualquer exceção | Só erros transitórios justificam retry; erros permanentes nunca se resolvem sozinhos |
| Fallback de deleção silencioso | Lançar exceção na deleção | Arquivo órfão no S3 é menos grave que impedir a deleção do livro |
| `registerHealthIndicator: true` | Sem health indicator | Estado do CB aparece no Actuator; integrável com alertas no Grafana |
| Fixed backoff 500ms | Exponential backoff com jitter | Suficiente para instância única; jitter relevante em múltiplas instâncias |
| CB sem Retry no S3 / Retry sem CB no banco | CB + Retry em tudo | Estratégia proporcional ao problema — cada padrão onde faz sentido |
