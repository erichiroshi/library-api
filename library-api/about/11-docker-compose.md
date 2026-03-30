# Docker Compose — Library API

## O que este documento cobre

Como o ambiente local é orquestrado com Docker Compose: os serviços, a separação por profiles, volumes, healthchecks, dependências entre containers, e como a composição evolui da Fase 2 (monolito) para a Fase 3 (microservices).

---

## Por que Docker Compose

Rodar a Library API localmente sem Docker exige instalar e configurar manualmente PostgreSQL, Redis, LocalStack (S3), Zipkin, Prometheus e Grafana — cada um com sua versão específica, configurações e dados de seed. Docker Compose encapsula tudo isso em um único arquivo declarativo.

```bash
docker compose up -d    # sobe todo o ambiente
docker compose down     # destrói tudo
docker compose down -v  # destrói incluindo volumes (dados)
```

Um desenvolvedor novo clona o repositório, roda `docker compose up -d` e tem o ambiente completo em minutos — sem documentação de instalação, sem conflitos de versão.

---

## Estrutura dos Arquivos Compose

```
docker/
├── compose.yml                  # serviços de infraestrutura (sempre sobem)
├── compose.app.yml              # a aplicação em si (opcional — dev pode rodar na IDE)
├── compose.monitoring.yml       # Prometheus + Grafana (opcional)
└── compose.override.yml         # overrides locais (não commitado)
```

A separação em múltiplos arquivos permite composição seletiva:

```bash
# só infraestrutura (app roda na IDE)
docker compose up -d

# infraestrutura + app containerizada
docker compose -f compose.yml -f compose.app.yml up -d

# tudo incluindo monitoramento
docker compose -f compose.yml -f compose.monitoring.yml up -d
```

Isso é especialmente útil no desenvolvimento: a aplicação roda na IDE com hot reload e debug, enquanto os serviços de infraestrutura ficam em containers.

---

## compose.yml — Infraestrutura Base

```yaml
name: library-api

services:

  postgres:
    image: postgres:16-alpine
    container_name: library-postgres
    environment:
      POSTGRES_DB: library
      POSTGRES_USER: ${POSTGRES_USER:-library}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-library}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./postgres/init:/docker-entrypoint-initdb.d  # scripts de inicialização
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-library} -d library"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - library-network

  redis:
    image: redis:7-alpine
    container_name: library-redis
    command: redis-server --save 60 1 --loglevel warning
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 3
    networks:
      - library-network

  localstack:
    image: localstack/localstack:3
    container_name: library-localstack
    environment:
      SERVICES: s3
      DEFAULT_REGION: us-east-1
      AWS_DEFAULT_REGION: us-east-1
    ports:
      - "4566:4566"
    volumes:
      - localstack_data:/var/lib/localstack
      - ./localstack/init-s3.sh:/etc/localstack/init/ready.d/init-s3.sh
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:4566/_localstack/health"]
      interval: 15s
      timeout: 10s
      retries: 5
    networks:
      - library-network

  zipkin:
    image: openzipkin/zipkin:3
    container_name: library-zipkin
    ports:
      - "9411:9411"
    networks:
      - library-network

volumes:
  postgres_data:
  redis_data:
  localstack_data:

networks:
  library-network:
    driver: bridge
```

### Decisões de configuração

**`${POSTGRES_USER:-library}`**: sintaxe de variável com default. Se `POSTGRES_USER` estiver definida no ambiente, usa o valor — senão, usa `library`. Permite customização sem modificar o `compose.yml`.

**`volumes` nomeados** (`postgres_data`, `redis_data`): dados persistem entre `docker compose down` e `docker compose up`. Apenas `docker compose down -v` destrói os dados. Sem volumes nomeados, todos os dados seriam perdidos a cada `down`.

**Rede dedicada** (`library-network`): todos os serviços se comunicam pelo nome do container na rede interna (`postgres`, `redis`, `localstack`). Sem rede dedicada, os containers usariam a rede padrão do Docker — ainda funciona, mas a rede nomeada torna as dependências explícitas.

---

## Healthchecks

Healthchecks são essenciais para `depends_on` funcionar corretamente. Sem healthcheck, `depends_on` apenas garante que o container **iniciou** — não que o serviço dentro está **pronto para aceitar conexões**.

```yaml
postgres:
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U library -d library"]
    interval: 10s    # verifica a cada 10s
    timeout: 5s      # falha se não responder em 5s
    retries: 5       # após 5 falhas consecutivas, status = unhealthy
```

```yaml
app:
  depends_on:
    postgres:
      condition: service_healthy   # aguarda postgres estar healthy
    redis:
      condition: service_healthy
```

`condition: service_healthy` faz o container da aplicação aguardar até o PostgreSQL estar realmente pronto — não apenas iniciado. Sem isso, a aplicação poderia tentar conectar ao banco antes do PostgreSQL estar aceitando conexões, causando falha no startup.

---

## Inicialização do LocalStack (S3)

O LocalStack precisa de um script para criar o bucket S3 antes da aplicação subir:

```bash
# docker/localstack/init-s3.sh
#!/bin/bash
awslocal s3 mb s3://library-dev-bucket
awslocal s3api put-bucket-policy \
  --bucket library-dev-bucket \
  --policy '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::library-dev-bucket/*"
    }]
  }'
echo "S3 bucket created and configured"
```

O LocalStack executa automaticamente scripts em `/etc/localstack/init/ready.d/` após estar pronto. O bucket é criado antes da aplicação tentar fazer upload — sem essa inicialização, o primeiro upload falharia com "bucket not found".

---

## compose.app.yml — A Aplicação

```yaml
services:
  app:
    build:
      context: ..
      dockerfile: Dockerfile
    container_name: library-app
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/library
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER:-library}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:-library}
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PORT: 6379
      AWS_S3_ENDPOINT: http://localstack:4566
      AWS_S3_BUCKET: library-dev-bucket
      AWS_ACCESS_KEY_ID: test
      AWS_SECRET_ACCESS_KEY: test
      MANAGEMENT_TRACING_EXPORT_ZIPKIN_ENDPOINT: http://zipkin:9411/api/v2/spans
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      localstack:
        condition: service_healthy
    networks:
      - library-network
    restart: on-failure:3   # reinicia até 3 vezes em caso de falha
```

**`SPRING_PROFILES_ACTIVE: docker`**: profile específico para ambiente Docker. Diferente do `dev` (que aponta para localhost) e do `prod` (que usa variáveis de ambiente da AWS real).

**`AWS_ACCESS_KEY_ID: test`**: o LocalStack aceita qualquer credencial — o valor `test` é uma convenção.

**`restart: on-failure:3`**: se a aplicação falhar (não por `docker compose stop`), reinicia até 3 vezes. Útil se o PostgreSQL demorar mais que o esperado para estar disponível — mesmo com healthcheck, pode haver race condition.

---

## compose.monitoring.yml — Prometheus e Grafana

```yaml
services:
  prometheus:
    image: prom/prometheus:v2.51.0
    container_name: library-prometheus
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - ./prometheus/alerts.yml:/etc/prometheus/alerts.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.retention.time=7d'   # retém 7 dias de métricas
    ports:
      - "9090:9090"
    networks:
      - library-network

  grafana:
    image: grafana/grafana:10.4.0
    container_name: library-grafana
    environment:
      GF_SECURITY_ADMIN_USER: ${GRAFANA_USER:-admin}
      GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_PASSWORD:-admin}
      GF_USERS_ALLOW_SIGN_UP: false
    volumes:
      - ./grafana/provisioning:/etc/grafana/provisioning
      - grafana_data:/var/lib/grafana
    ports:
      - "3000:3000"
    depends_on:
      - prometheus
    networks:
      - library-network

volumes:
  prometheus_data:
  grafana_data:
```

**`storage.tsdb.retention.time=7d`**: o Prometheus retém métricas por 7 dias. Para desenvolvimento local, 7 dias é mais que suficiente — sem esse limite, o volume do Prometheus cresceria indefinidamente.

**Versões pinadas** (`prom/prometheus:v2.51.0`, `grafana/grafana:10.4.0`): em vez de `latest`, versões específicas garantem que todos os desenvolvedores usam o mesmo binário. `latest` pode mudar entre `docker compose pull` e introduzir incompatibilidades silenciosas.

---

## Dockerfile da Aplicação

```dockerfile
# Multi-stage build
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src

# baixa dependências em camada separada (cache eficiente)
RUN mvn dependency:go-offline -q
RUN mvn package -DskipTests -q

# imagem final — apenas JRE, sem Maven, sem sources
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# usuário não-root por segurança
RUN addgroup -S library && adduser -S library -G library
USER library

COPY --from=builder /app/target/library-api-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Multi-stage build

A imagem final não contém o código-fonte, o Maven, nem o JDK completo — apenas o JRE e o JAR. Isso reduz o tamanho da imagem de ~600MB (com JDK + Maven) para ~180MB (só JRE).

### Camadas Docker e cache

A ordem das instruções importa para o cache do Docker:

```dockerfile
COPY pom.xml .          # muda raramente → camada cacheada
RUN mvn dependency:...  # baixa deps → cacheado se pom.xml não mudou
COPY src ./src          # muda frequentemente → invalida cache daqui pra frente
RUN mvn package ...     # recompila só quando sources mudaram
```

Se apenas o código mudou (sem mudança no `pom.xml`), o Docker reutiliza a camada de dependências — o build não baixa todas as dependências novamente.

### Usuário não-root

`USER library` garante que o processo Java roda como usuário não-privilegiado. Se houver uma vulnerabilidade no JAR, o atacante não terá acesso de root ao container.

---

## Fase 3: Compose para Microservices

Na Fase 3, o `compose.yml` cresce para incluir todos os serviços de infraestrutura e os sete componentes:

```yaml
# compose.microservices.yml
services:

  config-server:
    build: ./microservices/config-server
    environment:
      CONFIG_REPO_URI: ${CONFIG_REPO_URI}
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8888/actuator/health"]
      interval: 15s
      retries: 5

  eureka-server:
    build: ./microservices/eureka-server
    depends_on:
      config-server:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8761/actuator/health"]
      interval: 10s
      retries: 5

  gateway:
    build: ./microservices/gateway
    ports:
      - "8080:8080"
    depends_on:
      eureka-server:
        condition: service_healthy

  auth-service:
    build: ./microservices/auth-service
    depends_on:
      eureka-server:
        condition: service_healthy
      postgres:
        condition: service_healthy

  catalog-service:
    build: ./microservices/catalog-service
    depends_on:
      eureka-server:
        condition: service_healthy
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy

  loan-service:
    build: ./microservices/loan-service
    depends_on:
      eureka-server:
        condition: service_healthy
      postgres:
        condition: service_healthy
      catalog-service:
        condition: service_healthy
```

### O problema de depends_on em microservices

`depends_on` com `service_healthy` garante que o container está healthy — mas não que o serviço se registrou no Eureka. O `loan-service` pode subir com o `catalog-service` healthy, mas antes de ele aparecer no registry do Eureka.

A solução é retry com backoff no Feign client — se o `catalog-service` não for encontrado no Eureka, a chamada falha e o Retry tenta novamente até o serviço aparecer.

### Escalando serviços localmente

```bash
# sobe 2 instâncias do catalog-service
docker compose -f compose.yml -f compose.microservices.yml \
  up -d --scale catalog-service=2
```

O Eureka registra ambas as instâncias. O Gateway distribui requisições entre elas via load balancer. Isso permite testar comportamento de escala horizontal localmente.

---

## Portas dos Serviços

```
8080  → gateway (ponto de entrada externo) / app monolito
8081  → auth-service (interno)
8082  → catalog-service (interno)
8083  → loan-service (interno)
8761  → eureka-server (dashboard)
8888  → config-server
5432  → postgres
6379  → redis
4566  → localstack
9411  → zipkin
9090  → prometheus
3000  → grafana
```

Serviços internos (auth, catalog, loan) expõem portas apenas para debugging em desenvolvimento. Em produção, apenas o Gateway estaria exposto externamente.

---

## Resumo das Decisões de Docker Compose

| Decisão | Alternativa | Por que esta |
|---|---|---|
| Múltiplos arquivos compose | Um arquivo monolítico | Composição seletiva — monitoramento opcional, app na IDE ou container |
| Volumes nomeados | Bind mounts / sem volumes | Dados persistem entre restarts; bind mounts têm problemas de permissão em Windows |
| Versões pinadas (não `latest`) | `latest` | Reproducibilidade; evita quebras silenciosas entre pulls |
| `condition: service_healthy` | `condition: service_started` | Garante que o serviço está pronto, não apenas iniciado |
| `restart: on-failure:3` | Sem restart policy | Resiste a race conditions no startup sem loop infinito |
| Multi-stage build no Dockerfile | Build single-stage | Imagem ~70% menor; sem código-fonte na imagem final |
| Camada de dependências separada | COPY tudo junto | Cache eficiente — rebuild não baixa dependências se `pom.xml` não mudou |
| Usuário não-root no container | Rodar como root | Segurança — comprometimento do processo não dá acesso root ao host |
| Profile `docker` separado de `dev` | Mesmo profile | `dev` usa localhost; `docker` usa nomes de container na rede interna |
| Script init para LocalStack | Criar bucket via código | Bucket existe antes da aplicação subir; sem lógica de "create if not exists" |
