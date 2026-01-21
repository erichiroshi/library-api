# Library API

![CI](https://github.com/erichiroshi/library-api/actions/workflows/ci.yml/badge.svg)
[![codecov](https://codecov.io/github/erichiroshi/library-api/graph/badge.svg?token=Y71AMP148X)](https://codecov.io/github/erichiroshi/library-api)
![Java](https://img.shields.io/badge/Java-25-red)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Redis](https://img.shields.io/badge/Redis-Cache-red)
![Docker](https://img.shields.io/badge/Docker-Enabled-blue)

API REST para gerenciamento de uma biblioteca, desenvolvida com **Spring Boot** e focada em boas práticas de arquitetura backend, segurança, testes, observabilidade e infraestrutura moderna.

Este projeto foi construído de forma incremental, simulando um ambiente próximo ao mundo real, cobrindo desde a modelagem de domínio até cache distribuído, CI/CD e monitoramento.

---

## Visão Geral

A **Library API** permite gerenciar livros, autores, categorias, usuários e empréstimos, fornecendo endpoints REST seguros, documentados e testados. O projeto segue uma arquitetura em camadas bem definida e utiliza tecnologias amplamente adotadas no ecossistema Java.

---

## Tecnologias Utilizadas

### Backend
- Java 25
- Spring Boot 4
- Spring Web (REST)
- Spring Data JPA
- Hibernate
- Lombok
- Spring Security
- JWT (JSON Web Token)
- Spring Cache

### Persistência
- PostgreSQL
- Flyway (versionamento de banco de dados)
- H2 (testes)

### Serialização e Mapeamento
- Jackson
- DTOs
- MapStruct
- Bean Validation (Jakarta Validation)

### Cache
- Redis
- Spring Data Redis

### Observabilidade
- Spring Boot Actuator
- Micrometer
- Prometheus
- Grafana

### Testes
- JUnit 5
- Mockito
- Spring Boot Test
- @DataJpaTest
- Testcontainers
- Testes de Integração

### Infraestrutura
- Docker
- Docker Compose
- pgAdmin

### Documentação e Qualidade
- Swagger / OpenAPI
- Logging estruturado
- CI/CD (pipeline automatizado)

---

## Funcionalidades Implementadas

- Modelagem de domínio completa (Biblioteca)
- CRUD de entidades principais
- Validação de dados de entrada
- Mapeamento entre entidades e DTOs
- Tratamento global de exceções com `@ControllerAdvice` e `ProblemDetail`
- Autenticação e autorização via JWT
- Cache distribuído com Redis
- Seeds de dados para ambiente de teste
- Testes automatizados (unitários e integração)
- Documentação automática da API
- Observabilidade e métricas

---

## Modelagem de Domínio

Principais entidades do sistema:

- Author
- Book
- Category
- User
- Loan
- LoanItem

Relacionamentos modelados com JPA seguindo boas práticas (lazy loading, chaves compostas, tabelas de associação).

---

## Rotas Principais

### Autenticação
- `POST /auth/login`

### Categorias
- `GET /categories`
- `GET /categories/{id}`
- `POST /categories`

### Livros
- `GET /books`
- `GET /books/{id}`
- `POST /books`

### Autores
- `GET /authors`
- `GET /authors/{id}`
- `POST /authors`

*(Demais rotas podem ser consultadas via Swagger)*

---

## Documentação da API

A documentação interativa está disponível via Swagger:

```
http://localhost:8080/swagger-ui.html
```

---

## Perfis de Execução

- `test`: utilizado para testes automatizados
  - Cache desabilitado
  - Flyway desabilitado
  - Banco em memória

---

## Seed de Dados (Perfil de Teste)

No perfil `test`, o projeto utiliza um **seed de dados** para facilitar:
- Testes manuais via Postman
- Simulação de cenários reais
- Validação de regras de negócio

---

## Como Clonar o Projeto

```bash
git clone https://github.com/erichiroshi/library-api.git
cd library-api
```

---

## Como Executar com Docker

```bash
docker-compose up -d
```

Serviços disponíveis:
- PostgreSQL
- Redis
- pgAdmin - http://localhost:5050/
- Prometheus - http://localhost:9090/
- Grafana - http://localhost:3000/

Rodar pela ide
- API: `http://localhost:8080`


---

## Executar Localmente 

```bash
./gradlew clean build
./gradlew bootRun
```

---

## Executar Testes

```bash
./gradlew test
./gradlew integrationTest
```

---

## Observabilidade

- Actuator:
  ```
  http://localhost:8080/actuator
  ```

- Métricas Prometheus:
  ```
  http://localhost:8080/actuator/prometheus
  ```

- Grafana: dashboards configurados para visualização de métricas

---

## CI/CD

O projeto conta com pipeline automatizado para:
- Build
- Execução de testes
- Validação de qualidade

---

## Boas Práticas Aplicadas

- Separação clara de camadas (Controller, Service, Repository)
- DTOs para evitar exposição de entidades
- Cache aplicado no nível de Service
- Profiles para isolar infraestrutura em testes
- Testes previsíveis e reproduzíveis
- Logs claros e padronizados

---

## Próximos Passos Possíveis

- Rate limiting
- Versionamento de API
- Auditoria (createdAt, updatedAt, createdBy)
- OpenTelemetry (tracing distribuído)
- Deploy em cloud

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

📜 **Boas práticas**
- Adicione testes unitários.  
- Documente suas alterações no código.  
- Use mensagens de commit seguindo o padrão **Conventional Commits**.

---

## 🔗 Referências e Créditos
Este projeto foi desenvolvido com foco em aprendizado profundo de backend Java moderno, simulando desafios reais encontrados em ambientes profissionais.

- Desenvolvido por [**Eric Hiroshi**](https://github.com/erichiroshi)
- Licença: [MIT](LICENSE)

---

<p align="center">
  <em>“Código limpo é aquele que expressa a intenção com simplicidade e precisão.”</em>
</p>

---
