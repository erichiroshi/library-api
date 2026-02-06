# Library-API

![CI](https://github.com/erichiroshi/library-api/actions/workflows/ci.yml/badge.svg)
![CI](https://github.com/erichiroshi/library-api/actions/workflows/readme-pdf.yml/badge.svg)
[![codecov](https://codecov.io/github/erichiroshi/library-api/graph/badge.svg?token=Y71AMP148X)](https://codecov.io/github/erichiroshi/library-api)
![Java](https://img.shields.io/badge/Java-25-red)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Redis](https://img.shields.io/badge/Redis-Cache-red)
![Docker](https://img.shields.io/badge/Docker-Enabled-blue)

API REST desenvolvida em Java com **Spring Boot**, projetada para simular um backend de produção, aplicando boas práticas de arquitetura, segurança, testes automatizados, observabilidade e CI/CD.

O projeto tem como objetivo consolidar conhecimentos em desenvolvimento backend moderno, indo além de CRUDs simples, com foco em qualidade de código, manutenibilidade e confiabilidade.

---

## Visão Geral

A **Library API** permite gerenciar livros, autores, categorias, usuários e empréstimos, fornecendo endpoints REST seguros, documentados e testados. O projeto segue uma arquitetura em camadas bem definida e utiliza tecnologias amplamente adotadas no ecossistema Java.

---

## Tecnologias Utilizadas

### Backend
- **Java 25**
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
- **H2** (Banco de testes)

### Serialização e Mapeamento
- **Jackson** (Serialização e desserialização JSON)
- **DTOs** (Isolamento do modelo de domínio)
- **MapStruct** (Mapeamento automático)
- **Bean Validation (Jakarta Validation)** (Validação declarativa de entrada)

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

---

## Funcionalidades e Diferenciais

- Autenticação e autorização com JWT
- Cache distribuído com Redis usando Spring Cache
- Versionamento de banco de dados com Flyway
- Tratamento global de exceções com `@ControllerAdvice` e `ProblemDetail`
- Logs estruturados para rastreabilidade
- Métricas de aplicação expostas via Actuator
- Monitoramento com Prometheus e dashboards no Grafana
- Testes unitários e de integração com banco real via Testcontainers
- Pipeline CI/CD com verificação automática de cobertura mínima de testes

---

## Testes Automatizados

O projeto possui uma estratégia de testes dividida em:

- **Testes unitários**: validação de regras de negócio e serviços
- **Testes de repositório**: usando `@DataJpaTest`
- **Testes de integração**: com PostgreSQL real via Testcontainers

A cobertura de código é monitorada com **JaCoCo**, com threshold mínimo configurado.  
O pipeline falha automaticamente caso a cobertura fique abaixo do valor definido.

---

## Cache com Redis

O cache é aplicado na camada de serviço utilizando `@Cacheable`, garantindo:

- Separação entre lógica de negócio e camada HTTP
- Reutilização do cache por diferentes fluxos
- Melhor desempenho em consultas frequentes

Durante testes automatizados, o comportamento de cache é isolado para garantir previsibilidade e confiabilidade dos testes.

---

## Observabilidade

A aplicação expõe métricas através do Spring Actuator e Micrometer, permitindo:

- Monitoramento de performance
- Contagem de eventos de negócio
- Integração com Prometheus
- Visualização via Grafana

Exemplo de métrica customizada:
- Quantidade de livros criados

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
- PostgreSQL - localhost:5432
- Redis - localhost:6379
- pgAdmin - http://localhost:5050/
- Prometheus - http://localhost:9090/
- Grafana - http://localhost:3000/ (login admin/admin)

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

**Boas práticas**
- Adicione testes unitários.  
- Documente suas alterações no código.  
- Use mensagens de commit seguindo o padrão **Conventional Commits**.

---

## Referências e Créditos
Este projeto foi desenvolvido com foco em aprendizado profundo de backend Java moderno, simulando desafios reais encontrados em ambientes profissionais.

- Desenvolvido por [**Eric Hiroshi**](https://github.com/erichiroshi)
- LinkedIn: [**Eric Hiroshi**](https://www.linkedin.com/in/eric-hiroshi/)
- Licença: [MIT](LICENSE)

---

<p align="center">
  <em>“Código limpo é aquele que expressa a intenção com simplicidade e precisão.”</em>
</p>

---


