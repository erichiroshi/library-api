# Fase 4 — Etapa 1: RabbitMQ

**Branch:** `microservices`  
**Commit:** `feat(rabbitmq): adicionar mensageria assíncrona para restauração de cópias`  
**Status:** ✅ Concluída

---

## O que foi feito

- RabbitMQ `4.0.7-management` adicionado ao `docker-compose.yml` e `docker-compose.dev.yml`
- Exchange `library.events` (Direct) com Dead Letter Queue `library.events.dlq`
- `loan-service`: `LoanEventPublisher` publica `BookRestoreEvent` na devolução e cancelamento
- `catalog-service`: `BookRestoreConsumer` restaura cópias ao receber evento
- Chamada síncrona Feign (`bookClient.restoreCopies`) substituída por evento assíncrono
- Retry com backoff exponencial: 3 tentativas, 1s → 2s → 4s
- Testes unitários: `LoanEventPublisherTest` e `BookRestoreConsumerTest`

---

## Arquitetura da mensageria
```
loan-service
    │
    │ publishBookRestore(BookRestoreEvent)
    ↓
RabbitMQ
    Exchange: library.events (Direct)
    Routing Key: loan.book.restore
    │
    ↓
    Queue: catalog.book.restore
    │  ↘ falha após 3 tentativas
    │    Queue: catalog.book.restore.dlq
    ↓
catalog-service (BookRestoreConsumer)
    │
    ↓
    BookRepository.saveAll() — restaura cópias
```

---

## Decisões e Tradeoffs

### Síncrono (Feign) vs Assíncrono (RabbitMQ)

| | Feign síncrono | RabbitMQ assíncrono |
|---|---|---|
| Consistência | Imediata | Eventual |
| Resiliência | catalog fora = devolução falha | catalog fora = mensagem na fila |
| Acoplamento temporal | Alto | Baixo |
| Complexidade | Baixa | Média |
| Observabilidade de falhas | Log de erro | DLQ explícita |

**Por que assíncrono para restore:** a restauração de cópias não precisa acontecer na mesma transação da devolução. O usuário já tem o retorno — se o catalog processar 2 segundos depois, não há impacto para ele.

**Mantido síncrono:** `decrementCopies` no `create` — precisa ser síncrono pois a resposta depende do resultado (livro disponível ou não).

---

### Dead Letter Queue

Mensagens que falharam após 3 tentativas vão para `catalog.book.restore.dlq`. Em produção, um job monitora a DLQ e alerta o time — sem perda silenciosa de eventos.

---

### Backoff exponencial
```yaml
initial-interval: 1000ms
multiplier: 2.0
max-attempts: 3
# tentativas: t=0s, t=1s, t=3s → DLQ
```

Evita sobrecarga no `catalog-service` em caso de instabilidade temporária.

---

## Filas e exchanges criados

| Nome | Tipo | Função |
|---|---|---|
| `library.events` | Direct Exchange | Exchange principal |
| `library.events.dlq` | Direct Exchange | Exchange da DLQ |
| `catalog.book.restore` | Queue | Mensagens de restore |
| `catalog.book.restore.dlq` | Queue | Mensagens com falha |

---

## Verificação
```bash
# Management UI
http://localhost:15672 (guest/guest)

# Testar fluxo completo
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"joao.silva@email.com","password":"123456"}' \
  | jq -r '.access_token')

LOAN_ID=$(curl -s -X POST http://localhost:8080/api/v1/loans \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"booksId": [1]}' | jq -r '.id')

curl -X PATCH http://localhost:8080/api/v1/loans/$LOAN_ID/return \
  -H "Authorization: Bearer $TOKEN"
# → evento publicado no RabbitMQ → consumer restaura cópias
```

---

## Arquivos criados/modificados

| Arquivo | Ação |
|---|---|
| `docker-compose.yml` | Atualizado — rabbitmq adicionado |
| `docker-compose.dev.yml` | Atualizado — rabbitmq adicionado |
| `config-repo/application.yml` | Atualizado — spring.rabbitmq |
| `config-repo/application-dev.yml` | Atualizado — spring.rabbitmq localhost |
| `config-repo/catalog-service.yml` | Atualizado — retry config |
| `loan-service/config/RabbitMQConfig.java` | Criado |
| `loan-service/messaging/event/BookRestoreEvent.java` | Criado |
| `loan-service/messaging/LoanEventPublisher.java` | Criado |
| `loan-service/LoanService.java` | Modificado — publica evento em vez de Feign |
| `catalog-service/config/RabbitMQConfig.java` | Criado |
| `catalog-service/messaging/event/BookRestoreEvent.java` | Criado |
| `catalog-service/messaging/BookRestoreConsumer.java` | Criado |
| `loan-service/.../LoanEventPublisherTest.java` | Criado |
| `catalog-service/.../BookRestoreConsumerTest.java` | Criado |

---

## Próxima etapa

**Fase 4 — Etapa 2: Testes de carga (k6 + JMeter)**

- Cenário A: monolito sem cache
- Cenário B: monolito com cache Redis
- Cenário C: microservices sem cache
- Cenário D: microservices com cache Redis

## Lembrete

Ao final de todas as etapas da Fase 4, implementar mensageria em todos os eventos de loan:
- `LoanCreatedEvent` → notificações
- `LoanReturnedEvent` → restore (já implementado)
- `LoanCanceledEvent` → restore (já implementado)
Com DLQ e retry completo em todos.

---

## Etapa 2 — Testes de Carga (k6 + JMeter)

### Estrutura de pastas

```
load-tests/
├── k6/
│   ├── scripts/
│   │   ├── auth.js              ← helper de login
│   │   ├── scenario-a.js        ← monolito sem cache
│   │   ├── scenario-b.js        ← monolito com cache
│   │   ├── scenario-c.js        ← microservices sem cache
│   │   └── scenario-d.js        ← microservices com cache
│   ├── config/
│   │   └── load-profile.js      ← perfis de carga
│   └── results/
│       ├── scenario-a/
│       ├── scenario-b/
│       ├── scenario-c/
│       └── scenario-d/
├── jmeter/
│   ├── library-api-test.jmx     ← plano de teste JMeter
│   └── results/
├── reports/
│   └── comparative-analysis.md  ← análise final
└── run_tests.sh                 ← script principal