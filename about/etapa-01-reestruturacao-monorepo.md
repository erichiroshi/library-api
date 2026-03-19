# Etapa 1 — Reestruturação do Repositório para Monorepo

**Branch:** `microservices`  
**Commit:** `chore(repo): reestruturar repositório para monorepo multi-project`  
**Status:** ✅ Concluída

---

## O que foi feito

- Criação da branch `microservices` a partir de `main` (v1.3.1 estável)
- Movimentação do monolito para `library-api/`
- Configuração do Gradle multi-project na raiz
- Criação das pastas para os serviços da Fase 3
- Ajuste de todos os workflows de CI/CD
- Criação do README raiz e do `readme_pdf.md` raiz
- Definição de regras de evolução do repositório

---

## Estrutura resultante
```
library-api/               ← raiz do repositório
├── library-api/           ← monolito (v1.3.1, congelado)
├── config-server/         ← vazio (Etapa 2)
├── eureka-server/         ← vazio (Etapa 3)
├── gateway/               ← vazio (Etapa 5)
├── auth-service/          ← vazio (Etapa 6)
├── catalog-service/       ← vazio (Etapa 7)
├── loan-service/          ← vazio (Etapa 8)
├── config-repo/           ← vazio (Etapa 2)
├── about/                 ← documentação das etapas
├── settings.gradle        ← Gradle multi-project
├── build.gradle           ← configurações compartilhadas
├── README.md              ← vitrine do repositório
└── readme_pdf.md          ← fonte do PDF gerado no CI
```

---

## Decisões e Tradeoffs

### Monorepo vs Polyrepo

**Decisão:** Monorepo com Gradle multi-project.

| | Monorepo | Polyrepo |
|---|---|---|
| Refatoração entre serviços | Fácil — tudo visível | Difícil — PRs cruzados |
| Configuração de build compartilhada | Sim — `build.gradle` raiz | Não — cada repo repete |
| CI/CD | Um pipeline por serviço no mesmo repo | Um pipeline por repositório |
| Visibilidade para recrutadores | Um repositório conta a história completa | Fragmentado |
| Complexidade operacional | Baixa para portfólio | Alta — múltiplos repos para gerenciar |

**Por que monorepo para portfólio:** Um recrutador ou tech lead que acessa o repositório vê a evolução completa — do monolito aos microservices — em um único lugar. Polyrepo faria sentido para times grandes com domínios completamente independentes, o que não é o caso aqui.

---

### Gradle multi-project vs builds independentes

**Decisão:** Gradle multi-project com `settings.gradle` na raiz incluindo cada subprojeto.

**Tradeoff principal:** O monolito continua funcionando standalone com `./gradlew` dentro de `library-api/` — isso foi uma exigência para não quebrar o CI existente. O `settings.gradle` do subprojeto foi esvaziado (comentado o `rootProject.name`) para evitar conflito com o `settings.gradle` raiz.

**Regra definida:** cada novo serviço é incluído no `settings.gradle` raiz no momento da sua criação.

---

### Monolito congelado

**Decisão:** `library-api/` é tratado como congelado a partir deste ponto.

- Nenhuma feature nova será adicionada ao monolito
- O CI do monolito continua rodando para garantir que nada quebrou
- O `readme_pdf.md` do monolito não passa mais pelo CI — o monolito não será mais alterado
- Bugs críticos seriam corrigidos em `main` antes de qualquer merge da branch `microservices`

---

### CI/CD ajustado

**Problema:** todos os workflows apontavam para `./gradlew` na raiz. Com o monolito em `library-api/`, todos quebrariam.

**Solução:** `defaults.run.working-directory: library-api` no `ci.yml` — todos os steps `run:` passam a executar dentro de `library-api/` sem repetir o caminho em cada step.

Para o `docker.yml` a abordagem foi diferente porque o Docker build não usa `defaults.run` — foi necessário especificar `context: library-api` e `file: library-api/Dockerfile` explicitamente no step de build.

---

### README em dois níveis

**Decisão:** dois READMEs com responsabilidades distintas.

| Arquivo | Responsabilidade |
|---|---|
| `README.md` (raiz) | Vitrine do repositório — apresenta o projeto como um todo, links para cada serviço, status da Fase 3 |
| `library-api/readme.md` | Documentação técnica detalhada do monolito — stack, endpoints, decisões arquiteturais, como rodar |

O GitHub exibe o README da raiz na página inicial do repositório. Com o monolito em subpasta, sem um README raiz o repositório ficaria sem apresentação.

---

## Arquivos criados/modificados

| Arquivo | Ação |
|---|---|
| `README.md` | Criado — vitrine do repositório |
| `readme_pdf.md` | Criado — fonte do PDF da visão geral |
| `settings.gradle` | Criado — Gradle multi-project |
| `build.gradle` | Criado — configurações compartilhadas |
| `.gitignore` | Criado — cobrindo todos os subprojetos |
| `library-api/settings.gradle` | Modificado — `rootProject.name` comentado |
| `about/fase3/etapa-01-reestruturacao-monorepo.md` | Criado — este arquivo |
| `.github/workflows/ci.yml` | Modificado — `working-directory: library-api` |
| `.github/workflows/docker.yml` | Modificado — `context` e `file` apontando para `library-api/` |
| `.github/workflows/readme-pdf.yml` | Modificado — apenas PDF da raiz |
| `.github/dependabot.yml` | Modificado — `directory: "/library-api"` |

---

## Próxima etapa

**Etapa 2 — Config Server**

- Criar projeto Spring Boot em `config-server/`
- Configurar Spring Cloud Config Server
- Criar `config-repo/` com YAMLs por serviço
- Registrar `config-server` no `settings.gradle` raiz
- Adicionar entrada no `dependabot.yml`