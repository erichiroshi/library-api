# 📚 Library API — Spring Boot 4 + JWT + Docker + Observability

![CI](https://github.com/erichiroshi/library-api/actions/workflows/ci.yml/badge.svg)
![PDF](https://github.com/erichiroshi/library-api/actions/workflows/readme-pdf.yml/badge.svg)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=library-api&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=library-api)
[![codecov](https://codecov.io/github/erichiroshi/library-api/graph/badge.svg?token=Y71AMP148X)](https://codecov.io/github/erichiroshi/library-api)
![Java](https://img.shields.io/badge/Java-25-red)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Redis](https://img.shields.io/badge/Redis-Cache-red)
![Docker](https://img.shields.io/badge/Docker-Enabled-blue)

Backend projetado com foco em previsibilidade, observabilidade e isolamento de responsabilidades.

🔐 Autenticação JWT  
🧠 Arquitetura em camadas bem definida  
🗄 PostgreSQL + Flyway  
⚡ Cache distribuído com Redis  
📊 Observabilidade com Micrometer + Prometheus + Grafana  
🧪 Testes unitários e integração com Testcontainers  
🚀 CI/CD com cobertura mínima obrigatória  

---

## Visão Geral

A **Library API** permite gerenciar livros, autores, categorias, usuários e empréstimos, fornecendo endpoints REST seguros, documentados e testados. O projeto segue uma arquitetura em camadas bem definida e utiliza tecnologias amplamente adotadas no ecossistema Java.

---

## 🚀 Quick Start

O projeto possui dois modos de execução:

- **dev** → ambiente voltado para desenvolvimento e avaliação
- **prod** → ambiente containerizado simulando produção

---

### Clone o projeto

```bash
git clone https://github.com/erichiroshi/library-api.git
cd library-api
```

### 🟢 Modo Desenvolvimento (recomendado para avaliação)

Nesse modo a infraestrutura é executada via Docker e a aplicação pode ser iniciada via container ou IDE.

### 1️⃣ Subir infraestrutura

```bash
docker compose -f docker-compose.dev.yml up -d
```
A rede `library-api_backend` é criada automaticamente.

Serviços iniciados:
- PostgreSQL: localhost:5432
- Redis: localhost:6379
- pgAdmin: http://localhost:5050 (login admin@admin.com/admin)
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (login admin/admin)

### 2️⃣ Subir aplicação
Opção A — Container:
```bash
docker build -t library-api .
docker run -d --network library-api_backend -p 8080:8080 --env-file .env.dev library-api
```

Opção B — IDE:
```bash
./graldew clean build
```
Refresh gradle project  
Executar a aplicação.

Acesse:
- API: http://localhost:8080/api/v1
- Swagger: http://localhost:8080/swagger-ui/index.html

Usário admin para teste: joao.silva@email.com senha: 123456

Características do profile `dev`
- Swagger habilitado
- Banco de dados populado com seed inicial
- Configuração voltada para testes manuais
- Logs detalhados

## 🏭 Modo Produção (simulado)

Executa toda a stack containerizada utilizando o profile prod.

```bash
docker compose up -d
```
Características do profile `prod`

- Swagger desabilitado
- Banco de dados inicial vazio
- Configuração mais restritiva
- Ambiente totalmente containerizado
- Stateless (JWT) + cache compartilhado (Redis)

**Caso queiro testar no perfil de `prod`, rode a mesma seed de `dev`, via cli-bash:
```bash
docker exec -i library-api-postgres-1 psql -U postgres -d library < seed_realistic_dataset.sql
```

## 🧯 Encerrar ambiente

Para encerrar o ambiente:
```bash
docker compose down
```

---

## Postman
### Importe sua API

Arquivo na pasta raiz para importar no postman, para testar a api.  
`Library-API.postman_collection.json`

---

## 🧠 Problema que este projeto resolve

Simula um backend real com:

- Controle de empréstimos
- Autenticação segura
- Cache em consultas frequentes
- Métricas expostas para monitoramento
- Versionamento de banco automatizado
- Vai além de um CRUD simples.

---

## 🏗 Decisões Arquiteturais
✔ Separação Controller / Service / Repository  
Evita vazamento de regra de negócio para camada HTTP.

✔ DTOs + MapStruct  
Isolamento de domínio e controle explícito de exposição.

✔ Cache no nível de serviço  
Independente da camada web.

✔ Testcontainers  
Banco real nos testes de integração.

✔ Threshold de cobertura  
Pipeline falha abaixo do mínimo definido.

---

## 🛠 Stack Tecnológica

### Core
- **Java 25 LTS**
- **Spring Boot**
  - Spring Web (API REST)
  - Spring Data JPA (persistência)
  - Spring Security (JWT)
  - Spring Cache (Redis)
- **Hibernate** (Mapeamento objeto-relacional)
- **Lombok** (Reduzir boilerplate)

### Persistência
- **PostgreSQL** (Banco relacional)
- **Flyway** (Versionamento de schema)

### Cache
- **Redis** (Cache distribuído)

### Observabilidade
- **Actuator + Micrometer + Prometheus + Grafana** (Observabilidade)

### Testes
- **Testcontainers** (Testes de integração)
- **JUnit 5 & Mockito** (Testes automatizados)
- **JaCoCo** (Cobertura de código com threshold mínimo)

### Infraestrutura
- **Docker & Docker Compose** (Ambiente local)

### Documentação e Qualidade
- **Swagger / OpenAPI** (Documentação)
- **Logging estruturado** (Verificar fluxo)
- **GitHub Actions** (CI/CD)

### Serialização e Mapeamento
- **Jackson** (Serialização e desserialização JSON)
- **DTOs** (Isolamento do modelo de domínio)
- **MapStruct** (Mapeamento automático)
- **Bean Validation (Jakarta Validation)** (Validação declarativa de entrada)

---

## 📊 Observabilidade

Fluxo:  
Application → Actuator → Micrometer → Prometheus → Grafana

Métricas customizadas:
- Livros criados
- Tempo de resposta
- Contadores de endpoints

---

## 🧪 Estratégia de Testes

Unit tests isolando regra de negócio  
@DataJpaTest para repositórios  
Integração com banco real  
Pipeline com validação automática  

Cobertura atual: 80%+

---

## 📦 Endpoints Principais

POST /auth/login  
GET /books  
POST /books  
GET /authors  
POST /categories

Documentação completa via Swagger.

---

## 📐 Arquitetura

```
Controller → Service → Repository → Database
```

Responsabilidades claramente delimitadas.
Sem anêmico acoplamento entre camadas.

## 📈 Métricas do Projeto

- ~8.000 linhas
- 120+ testes
- 25+ endpoints
- 6 serviços Docker

## 🔮 Próximos Passos

- AWS S3
- Rate limiting (Bucket4j ou Resilience4j)
- OpenTelemetry (tracing distribuído)
- Deploy em cloud (AWS ECS ou Render)
- Implementar HATEOAS
- Tracing distribuído
- Micro Serviços

---

## Autor
Eric Hiroshi  
Backend Engineer — Java / Spring
- LinkedIn: [**Eric Hiroshi**](https://www.linkedin.com/in/eric-hiroshi/)
- Licença: [MIT](LICENSE)

---

## Contribuições

Contribuições são sempre bem-vindas!  
Para contribuir:

1. Crie um fork do repositório.  
2. Crie uma branch de feature:  
   ```bash
   git checkout -b feature/nova-funcionalidade
   ```
3. Commit suas mudanças:  
   ```bash
   git commit -m "feat: nova funcionalidade"
   ```
4. Envie um Pull Request.  

**Boas práticas**
- Adicione testes unitários.  
- Documente suas alterações no código.  
- Use mensagens de commit seguindo o padrão **Conventional Commits**.

---

## Documentação em PDF

A versão em PDF da documentação técnica é gerada automaticamente via GitHub Actions
e está disponível na aba **Releases** do projeto.

---

<p align="center">
  <em>“Código limpo é aquele que expressa a intenção com simplicidade e precisão.”</em>
</p>
