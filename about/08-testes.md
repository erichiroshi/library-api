# Testes — Testcontainers, JaCoCo e Estratégia de Testes

## O que este documento cobre

A estratégia de testes do projeto: pirâmide de testes, testes unitários com mocks, testes de integração com Testcontainers (PostgreSQL e Redis reais), profile de testes, cobertura com JaCoCo, integração com SonarCloud e Codecov, e como a estratégia evolui na Fase 3.

---

## A Pirâmide de Testes

```
           /\
          /  \
         / E2E\         poucos, lentos, caros
        /──────\
       / Integr.\       médios — dependências reais
      /──────────\
     /  Unitários \     muitos, rápidos, baratos
    /______________\
```

O projeto segue essa pirâmide: base larga de testes unitários rápidos, camada de testes de integração com dependências reais, sem E2E automatizados (testado manualmente via Swagger/Postman).

### Dois profiles de teste

```
src/test/
├── java/
│   └── com/library/
│       ├── unit/           # testes unitários
│       │   ├── service/
│       │   └── ...
│       └── integration/    # testes de integração
│           ├── controller/
│           └── ...
```

```yaml
# application-test.yml (profile para testes unitários)
spring:
  cache:
    type: none              # cache desabilitado
  jpa:
    hibernate:
      ddl-auto: create-drop # schema criado/destruído por teste

# application-it.yml (profile para testes de integração)
spring:
  cache:
    type: redis             # Redis real via Testcontainers
```

A separação em profiles garante que testes unitários não dependem de infraestrutura e rodam em milissegundos, enquanto testes de integração usam containers reais mas são executados em pipeline separado.

---

## Testes Unitários

### O que testar unitariamente

Testes unitários focam na **lógica de negócio isolada** — sem banco, sem Redis, sem S3. Dependências externas são substituídas por mocks (Mockito).

```java
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock BookRepository repository;
    @Mock BookMapper mapper;
    @Mock MeterRegistry meterRegistry;
    @Mock Counter bookCreatedCounter;

    @InjectMocks BookService service;

    @Test
    void create_whenIsbnAlreadyExists_shouldThrowConflictException() {
        // arrange
        var dto = new BookCreateDTO("Clean Code", "9780132350884", 2008, 3, Set.of(1L), 1L);
        when(repository.existsByIsbn(dto.isbn())).thenReturn(true);

        // act & assert
        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ISBN already exists");

        verify(repository, never()).save(any());
    }
}
```

### O que vale testar unitariamente

- Regras de negócio: ISBN duplicado, cópias indisponíveis, usuário sem permissão
- Mapeamentos: DTO → Entidade, Entidade → DTO
- Cálculos: datas de devolução, multas por atraso
- Casos de borda: listas vazias, valores nulos, limites

### O que não vale testar unitariamente

- Queries JPA — isso é responsabilidade do teste de integração
- Configurações do Spring — são testadas subindo o contexto
- Serialização JSON — testada no teste de integração do controller

A regra prática: se o teste precisaria mockar mais de 3 ou 4 colaboradores, provavelmente o método está fazendo coisas demais (sinal de refatoração) ou o teste está no nível errado (deveria ser integração).

---

## Testcontainers

### O problema dos mocks de banco

Uma alternativa ao Testcontainers é mockar o repositório nos testes de integração. Mas isso tem um problema fundamental: o mock não é o PostgreSQL. Queries que funcionam no mock podem falhar em produção por diferenças de dialeto SQL, comportamento de constraints, ou locking.

**Testcontainers sobe instâncias reais de serviços em containers Docker** durante os testes — e os destrói ao final. O teste de integração roda contra o mesmo PostgreSQL 16 que roda em produção.

### Configuração

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("it")
@Testcontainers
class BookControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("library_test")
                    .withUsername("test")
                    .withPassword("test");

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }
}
```

`@DynamicPropertySource` injeta as URLs e portas dinâmicas dos containers no contexto do Spring — os containers sobem em portas aleatórias para evitar conflito com instâncias locais.

### `static` nos containers

Os containers são `static` intencionalmente: sobem uma vez para toda a classe de teste, não uma vez por método. Subir um container PostgreSQL leva 2-3 segundos — fazer isso para cada teste individual tornaria a suite inviável.

O trade-off é que os testes compartilham a mesma instância do banco. Para garantir isolamento, cada teste limpa os dados que criou:

```java
@AfterEach
void cleanup() {
    loanRepository.deleteAll();
    bookRepository.deleteAll();
    userRepository.deleteAll();
}
```

Ou com `@Transactional` no teste — o Spring faz rollback após cada método de teste, sem necessidade de cleanup manual.

### O que testar com Testcontainers

```java
@Test
void findAll_whenBooksExist_shouldReturnPaginatedList() {
    // arrange — persiste dados reais no PostgreSQL
    bookRepository.saveAll(List.of(
        buildBook("Clean Code", "9780132350884"),
        buildBook("The Pragmatic Programmer", "9780135957059")
    ));

    // act — requisição HTTP real contra o controller
    var response = restTemplate.getForEntity("/api/v1/books?size=10", PageResponseDTO.class);

    // assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().content()).hasSize(2);
}

@Test
void create_whenIsbnAlreadyExists_shouldReturn409() {
    bookRepository.save(buildBook("Clean Code", "9780132350884"));

    var dto = new BookCreateDTO("Clean Code 2nd", "9780132350884", ...);
    var response = restTemplate.postForEntity("/api/v1/books", dto, ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
}
```

Esses testes exercitam toda a stack: HTTP → Security → Controller → Service → Repository → PostgreSQL → volta. Se a constraint de ISBN único estiver mal configurada no banco, o teste de integração vai pegar — o unitário não.

### Testcontainers e Flyway

Com Testcontainers, o Flyway roda as migrações reais no container PostgreSQL antes dos testes. Isso garante que o schema de teste é idêntico ao de produção — incluindo índices, constraints e dados de seed do `dev`.

Se uma migração quebrar, o teste falha antes mesmo de começar. Isso torna o CI o primeiro lugar onde migrações com problema são detectadas — não em produção.

---

## JaCoCo — Cobertura de Código

### O que é e o que mede

JaCoCo (Java Code Coverage) instrumenta o bytecode da aplicação e mede quais linhas, branches e instruções foram executados durante os testes.

```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals><goal>report</goal></goals>
        </execution>
        <execution>
            <id>check</id>
            <goals><goal>check</goal></goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>  <!-- 80% mínimo -->
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**`prepare-agent`**: instrumenta o bytecode para rastrear execução.
**`report`**: gera o relatório HTML/XML em `target/site/jacoco/`.
**`check`**: falha o build se a cobertura estiver abaixo do mínimo configurado (80%).

### 80% — por que esse threshold?

80% de cobertura de linhas é um limiar comum na indústria — suficientemente alto para garantir que a lógica principal está coberta, sem exigir cobertura de código boilerplate (getters, construtores, configurações).

Buscar 100% de cobertura tem retornos decrescentes: os últimos 20% geralmente cobrem código trivial ou cenários impossíveis de testar de forma significativa. O esforço é melhor empregado em qualidade dos testes existentes.

### Exclusões

```xml
<configuration>
    <excludes>
        <!-- excluir classes que não têm lógica de negócio -->
        <exclude>**/dto/**</exclude>
        <exclude>**/config/**</exclude>
        <exclude>**/exception/**</exclude>
        <exclude>**/*Application.class</exclude>
        <exclude>**/mapper/**</exclude>
    </excludes>
</configuration>
```

DTOs, configs e a classe `main` não têm lógica testável — incluí-los na métrica inflacionaria artificialmente a cobertura (fácil de cobrir) ou a reduziria injustamente (construtores de record gerados).

---

## SonarCloud

O SonarCloud analisa o código além da cobertura — detecta code smells, bugs potenciais, vulnerabilidades de segurança, e código duplicado.

### Integração com JaCoCo

```yaml
# .github/workflows/sonar.yml
- name: Build and analyze
  env:
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
  run: mvn verify sonar:sonar
       -Dsonar.projectKey=erichiroshi_library-api
       -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
```

O SonarCloud consome o relatório XML do JaCoCo para exibir cobertura por classe e método no dashboard.

### Quality Gate

O Quality Gate é um conjunto de condições que o código deve satisfazer para ser considerado "aprovado":

```
Quality Gate — conditions:
✓ Coverage >= 80%
✓ Duplicated Lines < 3%
✓ Maintainability Rating: A
✓ Reliability Rating: A
✓ Security Rating: A
✓ Security Hotspots Reviewed: 100%
```

Se o Quality Gate falha, o CI bloqueia o merge — o badge no README muda para vermelho. Isso torna a qualidade de código uma condição objetiva do processo de desenvolvimento, não uma aspiração.

---

## Codecov

O Codecov complementa o SonarCloud com foco em **cobertura por pull request** — mostra exatamente quais linhas do diff não estão cobertas.

```yaml
# .github/workflows/ci.yml
- name: Upload coverage to Codecov
  uses: codecov/codecov-action@v4
  with:
    token: ${{ secrets.CODECOV_TOKEN }}
    files: target/site/jacoco/jacoco.xml
    fail_ci_if_error: true
```

No pull request, o Codecov comenta automaticamente:

```
Coverage Report
Base: 83.2%  Head: 84.1%  Diff: +0.9%

Files changed:
  BookService.java    85% → 87%  ✓
  LoanService.java    79% → 76%  ✗ (below threshold)
```

Isso fecha o loop: o desenvolvedor vê imediatamente quais linhas do código novo não foram testadas, antes do merge.

---

## Testcontainers e Performance no CI

Subir containers no CI tem custo de tempo. Estratégias para minimizar:

### Reuso de containers

```java
@Container
static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine")
                .withReuse(true);  // reusa o container entre execuções (dev local)
```

`withReuse(true)` é útil em desenvolvimento local — o container não é destruído ao fim dos testes e é reaproveitado na próxima execução. No CI, containers sempre sobem do zero para garantir isolamento entre builds.

### Imagens pré-baixadas no CI

```yaml
# .github/workflows/ci.yml
- name: Pull test images
  run: |
    docker pull postgres:16-alpine
    docker pull redis:7-alpine
```

Pré-baixar as imagens antes dos testes evita que o tempo de pull conte no tempo do teste. O GitHub Actions cacheia layers Docker entre execuções, tornando builds subsequentes mais rápidos.

---

## Fase 3: Testes nos Microservices

### Testes de contrato (Consumer-Driven Contract Testing)

Na Fase 3, com múltiplos serviços independentes, o problema de compatibilidade emerge: `loan-service` chama `catalog-service` via Feign. Se o `catalog-service` muda a resposta de um endpoint, o `loan-service` quebra — mas isso só seria descoberto em ambiente integrado.

**Spring Cloud Contract** (ou Pact) resolve isso com testes de contrato:

```
catalog-service (provider) define contratos:
  "GET /api/v1/books/1 retorna { id: 1, title: ..., availableCopies: ... }"

loan-service (consumer) verifica contra esses contratos em build time,
  sem precisar subir o catalog-service real.
```

O producer garante que seus contratos continuam válidos a cada build. O consumer verifica que usa a API conforme o contrato. Incompatibilidades são detectadas antes do deploy, não depois.

### Testcontainers + múltiplos serviços

Para testes de integração end-to-end na Fase 3, o Testcontainers pode subir toda a stack:

```java
@Container
static GenericContainer<?> configServer = new GenericContainer<>("library/config-server:latest")...;

@Container
static GenericContainer<?> eurekaServer = new GenericContainer<>("library/eureka-server:latest")...;

@Container
static GenericContainer<?> catalogService = new GenericContainer<>("library/catalog-service:latest")...;
```

Isso é mais pesado — mas garante que a integração real entre serviços funciona antes de chegar em produção.

---

## Resumo das Decisões de Testes

| Decisão | Alternativa | Por que esta |
|---|---|---|
| Testcontainers (PostgreSQL/Redis reais) | H2 em memória / mocks | H2 não é PostgreSQL; diferenças de dialeto escondem bugs reais |
| Containers `static` por classe | Container por método de teste | Performance — 2-3s por container × N testes = suite inviável |
| `@DynamicPropertySource` | Portas fixas no `application-it.yml` | Containers sobem em portas aleatórias; evita conflito com serviços locais |
| Cache desabilitado em testes unitários (`type: none`) | Cache habilitado | Testes unitários não devem depender de Redis; assertivas mais previsíveis |
| 80% de cobertura mínima | 100% ou sem threshold | 100% tem retorno decrescente; threshold impõe disciplina sem custo excessivo |
| Exclusão de DTOs/configs do JaCoCo | Incluir tudo | Evita inflacionar/deflacionar artificialmente a métrica de cobertura |
| Quality Gate no SonarCloud bloqueando merge | Apenas relatório sem bloqueio | Qualidade como condição objetiva, não aspiração |
| Codecov em PRs | Só relatório pós-merge | Feedback imediato no diff — o dev vê o que não cobriu antes do merge |
| Flyway rodando em testes de integração | Schema manual no H2 | Schema de teste idêntico ao de produção; migrações quebradas detectadas no CI |
