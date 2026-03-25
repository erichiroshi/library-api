# Etapa 6 — Auth Service

**Branch:** `microservices`  
**Commit:** `feat(auth-service): extrair contexto de autenticação do monolito`  
**Status:** ✅ Concluída

---

## O que foi feito

- Criado `auth-service/` via Spring Initializr (Spring Boot 4.0.4)
- Copiados domínios `auth/`, `user/`, `refresh_token/`, `security/` do monolito
- SecurityConfig adaptado — `permitAll()` pois Gateway já valida JWT
- `JwtAuthenticationFilter` removido — desnecessário no auth-service
- Migrations Flyway para schema `auth` (4 migrations)
- ShedLock para `RefreshTokenCleanupJob`
- Porta aleatória — registrado no Eureka como `auth-service`
- Adicionado ao `docker-compose.yml`

---

## Decisões e Tradeoffs

### permitAll() no SecurityConfig

**Decisão:** `auth-service` não revalida JWT — confia nos headers do Gateway.

O Gateway valida o JWT e propaga `X-User-Id` e `X-User-Roles`. O `auth-service` não precisa de `JwtAuthenticationFilter`. Spring Security ainda é necessário para o `AuthenticationManager` usado no login.

**Risco:** se alguém acessar o `auth-service` diretamente (bypassando o Gateway), não há proteção. Mitigado em produção com regras de rede — apenas o Gateway pode chamar os serviços internos.

### Migrations isoladas por serviço

Cada serviço tem suas próprias migrations — apenas as tabelas do seu schema. O `auth-service` gerencia apenas `auth.tb_user`, `auth.tb_user_roles` e `auth.tb_refresh_tokens`.

### ShedLock permanece em public

A tabela `shedlock` fica em `public` — é infraestrutura, não domínio. Todos os serviços que precisarem de lock distribuído usam a mesma tabela.
