# CI/CD — GitHub Actions

## O que este documento cobre

A estratégia de CI/CD do projeto: os quatro workflows, o que cada um faz e quando dispara, as decisões de design do pipeline, geração de PDF com LaTeX, versionamento semântico automatizado, publicação de releases, e como o pipeline evolui na Fase 3 com múltiplos serviços.

---

## Visão Geral dos Quatro Workflows

```
.github/workflows/
├── ci.yml           # validação contínua — todo push/PR
├── release.yml      # geração de release — tag v*.*.*
├── sonar.yml        # análise de qualidade — main/develop
└── dependency.yml   # verificação de dependências — semanal
```

Cada workflow tem responsabilidade única e dispara em momento diferente. Essa separação evita pipelines monolíticos que fazem tudo numa execução — e que quando falham, não deixam claro o que quebrou.

---

## Workflow 1: CI — Validação Contínua

```yaml
name: CI

on:
  push:
    branches: [ main, develop, 'feature/**', 'fix/**' ]
  pull_request:
    branches: [ main, develop ]
```

Dispara em todo push e em PRs para `main` e `develop`. É o workflow mais executado — e por isso precisa ser o mais rápido.

### Jobs e ordem de execução

```
build-and-test
      │
      ├── Setup JDK 25
      ├── Cache Maven dependencies
      ├── mvn verify (compila + testes unitários + JaCoCo)
      ├── Upload coverage to Codecov
      └── Upload test results (artifact)
```

### Cache de dependências Maven

```yaml
- name: Cache Maven packages
  uses: actions/cache@v4
  with:
    path: ~/.m2/repository
    key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
    restore-keys: |
      ${{ runner.os }}-maven-
```

A chave de cache inclui o hash do `pom.xml` — se as dependências mudarem, o cache é invalidado e refeito. Se o `pom.xml` não mudou, as dependências são restauradas do cache em segundos em vez de baixadas novamente.

Isso reduz o tempo do job de ~3 minutos (sem cache) para ~40 segundos (com cache).

### Testes de integração separados

Os testes de integração (Testcontainers) rodam em job separado:

```yaml
integration-tests:
  needs: build-and-test  # só roda se build-and-test passou
  runs-on: ubuntu-latest
  services:
    # não usa services do GitHub Actions para banco —
    # o Testcontainers sobe os containers por conta própria
  steps:
    - name: Run integration tests
      run: mvn verify -Pit -DskipUnitTests
```

**Por que separado?** Testes de integração são lentos (Testcontainers sobe containers). Separar permite ver rapidamente se o problema é no build/unitários ou na integração — e o feedback de build quebrado chega mais rápido.

### Artifact de resultados

```yaml
- name: Upload test results
  uses: actions/upload-artifact@v4
  if: always()  # mesmo se o job falhou
  with:
    name: test-results
    path: target/surefire-reports/
    retention-days: 7
```

`if: always()` garante que os relatórios de teste são salvos mesmo quando o job falha — exatamente quando mais são necessários para diagnóstico.

---

## Workflow 2: Release — Geração de Release

```yaml
name: Release

on:
  push:
    tags:
      - 'v*.*.*'   # dispara em tags semânticas: v1.0.0, v1.3.1, etc.
```

Este é o workflow mais complexo. Dispara quando uma tag semântica é criada — o que acontece manualmente pelo desenvolvedor ou via script de release.

### Jobs em paralelo

```
          tag push
              │
    ┌─────────┴──────────┐
    │                    │
build-jar           build-pdf
    │                    │
    └─────────┬──────────┘
              │
         create-release
              │
         publish-docker  (futuro)
```

`build-jar` e `build-pdf` rodam em paralelo — independentes entre si. O `create-release` aguarda ambos (`needs: [build-jar, build-pdf]`) e usa os artefatos gerados.

### Job: build-jar

```yaml
build-jar:
  steps:
    - name: Build JAR
      run: mvn package -DskipTests

    - name: Upload JAR artifact
      uses: actions/upload-artifact@v4
      with:
        name: library-api-jar
        path: target/library-api-*.jar
```

`-DskipTests` — os testes já rodaram no CI antes da tag ser criada. O release build não precisa rodar testes novamente.

### Job: build-pdf — LaTeX

Esta é a parte mais incomum do pipeline: geração de documentação em PDF a partir de LaTeX.

```yaml
build-pdf:
  runs-on: ubuntu-latest
  steps:
    - name: Install LaTeX
      run: |
        sudo apt-get update
        sudo apt-get install -y texlive-full latexmk

    - name: Build PDF
      run: latexmk -pdf -output-directory=pdf docs/readme_pdf.tex

    - name: Upload PDF artifact
      uses: actions/upload-artifact@v4
      with:
        name: library-api-docs
        path: pdf/readme_pdf.pdf
```

#### Por que LaTeX para documentação?

O PDF de documentação técnica do projeto é gerado a partir de um arquivo `.tex` — não de Markdown convertido. LaTeX oferece:

- Controle tipográfico preciso (espaçamento, fontes, layout)
- Índice automático com hyperlinks
- Código-fonte com syntax highlighting via `minted`
- Numeração de páginas, cabeçalhos, rodapés customizados
- Resultado visualmente profissional — diferente de um Markdown convertido com `pandoc`

O `texlive-full` é uma instalação pesada (~4GB), o que torna este job o mais lento do pipeline (~8-10 minutos). Mas roda em paralelo com o `build-jar`, então não bloqueia o `create-release`.

### Job: create-release

```yaml
create-release:
  needs: [build-jar, build-pdf]
  steps:
    - name: Download artifacts
      uses: actions/download-artifact@v4

    - name: Extract version from tag
      run: echo "VERSION=${GITHUB_REF#refs/tags/}" >> $GITHUB_ENV

    - name: Generate changelog
      id: changelog
      uses: metcalfc/changelog-generator@v4.3.1
      with:
        myToken: ${{ secrets.GITHUB_TOKEN }}

    - name: Create GitHub Release
      uses: softprops/action-gh-release@v2
      with:
        tag_name: ${{ env.VERSION }}
        name: "Library API ${{ env.VERSION }}"
        body: ${{ steps.changelog.outputs.changelog }}
        files: |
          library-api-jar/library-api-*.jar
          library-api-docs/readme_pdf.pdf
        draft: false
        prerelease: ${{ contains(env.VERSION, '-') }}  # v1.0.0-beta é pre-release
```

`prerelease: ${{ contains(env.VERSION, '-') }}` — tags como `v1.0.0-beta` ou `v1.0.0-RC1` são automaticamente marcadas como pre-release no GitHub. Tags sem sufixo (`v1.3.1`) são releases estáveis.

### Changelog automático

O `changelog-generator` agrupa commits desde a última tag em categorias baseadas em Conventional Commits:

```markdown
## What's Changed

### Features
- feat: adiciona endpoint de renovação de empréstimo (#45)
- feat: implementa cache Redis para busca de livros (#43)

### Bug Fixes
- fix: corrige validação de ISBN com hífens (#47)

### Documentation
- docs: atualiza README com instruções de deploy (#46)
```

Isso só funciona bem quando os commits seguem Conventional Commits — que é exatamente o padrão adotado no projeto.

---

## Workflow 3: Sonar — Análise de Qualidade

```yaml
name: SonarCloud Analysis

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]
```

Dispara apenas em branches principais e em PRs para elas — não em toda feature branch. Análise estática consome créditos do SonarCloud e tempo; rodá-la em toda branch seria desperdício.

```yaml
steps:
  - name: Build, test and analyze
    env:
      GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
      SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
    run: |
      mvn verify sonar:sonar \
        -Dsonar.projectKey=erichiroshi_library-api \
        -Dsonar.organization=erichiroshi \
        -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
```

O `mvn verify` roda os testes e gera o relatório JaCoCo. O `sonar:sonar` envia o relatório e o código para análise no SonarCloud.

### Integração com PRs

Quando o Sonar roda em um PR, ele comenta automaticamente no GitHub com o resultado do Quality Gate:

```
SonarCloud Quality Gate: ✅ Passed
Coverage: 83.4% (threshold: 80%)
New Issues: 0
Security Hotspots: 0
```

Se o Quality Gate falha, o status check do PR fica vermelho — prevenindo merge de código abaixo do padrão.

---

## Workflow 4: Dependency Check — Segurança de Dependências

```yaml
name: Dependency Check

on:
  schedule:
    - cron: '0 8 * * 1'  # toda segunda-feira às 8h UTC
  workflow_dispatch:       # permite execução manual
```

Roda semanalmente, não a cada push. Dependências não mudam a cada commit — verificação semanal é frequência suficiente para detectar vulnerabilidades novas em dependências existentes.

```yaml
steps:
  - name: OWASP Dependency Check
    uses: dependency-check/Dependency-Check_Action@main
    with:
      project: 'library-api'
      path: '.'
      format: 'HTML'
      args: >
        --failOnCVSS 7
        --enableRetired

  - name: Upload report
    uses: actions/upload-artifact@v4
    with:
      name: dependency-check-report
      path: reports/
```

`--failOnCVSS 7` — falha o workflow se alguma dependência tiver vulnerabilidade com CVSS score ≥ 7 (alta ou crítica). Vulnerabilidades de baixa severidade (< 7) geram relatório mas não falham o build.

`workflow_dispatch` permite rodar manualmente — útil quando uma vulnerabilidade crítica é divulgada e não se quer esperar pela execução semanal.

---

## Estratégia de Branching

O pipeline foi projetado para o seguinte fluxo:

```
feature/xyz  →  develop  →  main  →  tag v*.*.*  →  release
    │               │          │
    CI              CI         CI + Sonar
    (rápido)        + Sonar
```

- **feature branches**: apenas CI (build + testes)
- **develop**: CI + Sonar (Quality Gate)
- **main**: CI + Sonar + gatilho para release quando tagged
- **tag**: workflow de release completo (JAR + PDF + GitHub Release)

### Por que não deploy automático?

O projeto é um portfólio/aprendizado — não há ambiente de produção real para deploy automático. O pipeline vai até a criação da release no GitHub. Deploy para um ambiente real (ECS, EKS, Railway, etc.) seria o próximo passo natural.

Na Fase 3, o pipeline seria estendido para:

```
tag → build imagens Docker → push para ECR/GHCR → deploy para ECS/EKS
```

---

## Secrets e Segurança no Pipeline

```yaml
env:
  SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
  CODECOV_TOKEN: ${{ secrets.CODECOV_TOKEN }}
  GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}  # automático, não precisa configurar
```

- `SONAR_TOKEN`: token da conta SonarCloud — configurado nos secrets do repositório
- `CODECOV_TOKEN`: token do Codecov — idem
- `GITHUB_TOKEN`: gerado automaticamente pelo GitHub Actions para cada execução — usado para criar releases, comentar em PRs, etc.

Nenhum segredo é hard-coded no workflow — todos vêm de `secrets.*`. O GitHub impede que valores de secrets apareçam nos logs, mesmo se acidentalmente logados.

### Permissões mínimas

```yaml
permissions:
  contents: write    # necessário para criar release
  pull-requests: write  # necessário para comentar no PR
  checks: write      # necessário para status checks
```

Workflows usam permissões mínimas necessárias — princípio do menor privilégio aplicado ao CI/CD.

---

## Fase 3: CI/CD para Microservices

Na Fase 3 com monorepo e sete componentes, o pipeline precisa ser repensado. Recompilar e testar todos os serviços a cada push é desperdício — se apenas o `catalog-service` mudou, apenas ele precisa ser testado e publicado.

### Path filtering

```yaml
# ci-catalog-service.yml
on:
  push:
    paths:
      - 'microservices/catalog-service/**'
      - 'microservices/shared-lib/**'  # mudança na lib compartilhada afeta todos
```

Cada serviço tem seu próprio workflow que dispara apenas quando arquivos relevantes mudam.

### Pipeline por serviço

```
push em catalog-service/
        │
        ├── build + test catalog-service
        ├── build Docker image
        ├── push para GHCR (GitHub Container Registry)
        └── deploy para ambiente de staging
```

### Config-repo como gatilho

Mudanças no `config-repo` (configurações externas do Spring Cloud Config) disparam um workflow específico:

```yaml
# ci-config.yml
on:
  push:
    paths:
      - 'config-repo/**'
steps:
  - name: Validate YAML syntax
    run: find config-repo -name '*.yml' -exec python -c "import yaml; yaml.safe_load(open('{}'))" \;
  - name: Trigger rolling restart
    # notifica os serviços para recarregar configuração via Spring Cloud Bus
```

Validar YAML antes de fazer push para o config-repo evita que uma configuração malformada quebre todos os serviços que dependem dela.

---

## Resumo das Decisões de CI/CD

| Decisão | Alternativa | Por que esta |
|---|---|---|
| Quatro workflows separados | Um workflow monolítico | Responsabilidade única; falha localizada; execução seletiva |
| Cache de dependências Maven | Sem cache | Reduz tempo de ~3min para ~40s por execução |
| Testes de integração em job separado | Junto com unitários | Feedback mais rápido; problema localizado (unitário vs integração) |
| `if: always()` nos artifacts de teste | Sem artifacts | Relatórios disponíveis exatamente quando o build falha |
| LaTeX para PDF | Pandoc/Markdown | Controle tipográfico; resultado profissional; syntax highlighting |
| build-jar e build-pdf em paralelo | Sequencial | LaTeX é lento (~10min); paralelismo elimina esse bottleneck |
| Changelog via Conventional Commits | Changelog manual | Automático e consistente quando commits seguem o padrão |
| Sonar apenas em main/develop/PR | Em toda branch | Créditos do SonarCloud; relevante apenas onde há review/merge |
| Dependency check semanal | A cada push | Frequência adequada para descoberta de novas CVEs; não polui CI diário |
| `--failOnCVSS 7` | Falhar em qualquer CVE | Vulnerabilidades baixas são barulho; alta/crítica exigem ação imediata |
| `workflow_dispatch` no dependency check | Apenas schedule | Permite execução manual em caso de CVE crítico divulgado |
| Permissões mínimas por workflow | `permissions: write-all` | Princípio do menor privilégio; comprometimento de um workflow não afeta outros |
