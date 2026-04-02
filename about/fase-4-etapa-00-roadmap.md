# Fase 4 — Performance, Mensageria e Posicionamento

## Objetivo
Demonstrar raciocínio de performance com números reais,
implementar mensageria com RabbitMQ e construir
posicionamento profissional estratégico.

## Etapas

### Etapa 1 — RabbitMQ
- [x] Adicionar RabbitMQ ao docker-compose.yml
- [x] Implementar producer no loan-service (LoanReturnedEvent, LoanCanceledEvent)
- [x] Implementar consumer no catalog-service (restaurar cópias)
- [x] Configurar Dead Letter Queue (DLQ)
- [x] Configurar retry com backoff exponencial
- [x] Substituir chamada Feign síncrona por evento assíncrono na devolução
- [x] Testes unitários dos producers e consumers
- [x] Commit: `feat(rabbitmq): adicionar mensageria assíncrona entre loan e catalog service`

### Etapa 2 — Testes de carga (k6)
- [ ] Instalar k6
- [ ] Criar script base: fluxo completo de empréstimo
- [ ] Cenário A: monolito sem cache
- [ ] Cenário B: monolito com cache Redis
- [ ] Cenário C: microservices sem cache
- [ ] Cenário D: microservices com cache Redis
- [ ] Script `run_tests.sh` para automatizar todos os cenários
- [ ] Commit: `test(k6): adicionar testes de carga para todos os cenários`

### Etapa 3 — Análise dos resultados
- [ ] Coletar métricas: RPS, P50, P95, P99, taxa de erro
- [ ] Gerar relatório comparativo em markdown
- [ ] Dashboard Grafana com comparativo lado a lado
- [ ] Screenshots para LinkedIn e currículo
- [ ] Commit: `docs(performance): adicionar análise comparativa de performance`

### Etapa 4 — Currículo estratégico
- [ ] Currículo em português (método STAR)
- [ ] Currículo em inglês (método STAR)
- [ ] Bullet points técnicos com números reais
- [ ] Seção de projetos com links e métricas
- [ ] Commit: `docs(career): adicionar currículo estratégico com método STAR`