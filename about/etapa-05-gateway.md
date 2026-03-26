# Etapa 5 — Gateway

**Branch:** `microservices`  
**Commit:** `feat(gateway): adicionar Spring Cloud Gateway com JWT centralizado`  
**Status:** ✅ Concluída

---

## O que foi feito

- Gerado `gateway/` via Spring Initializr (Spring Boot 4.0.4)
- Implementado `JwtAuthenticationFilter` como `HandlerFilterFunction`
- Implementado `JwtService` — extrai claims sem consulta ao banco
- Rotas configuradas no `config-repo/gateway.yml`
- Porta fixa `8080`
- Registrado no Eureka
- Adicionado ao `docker-compose.yml`
- `dependabot.yml` atualizado

---

## Estrutura criada
```
gateway/
├── src/main/java/com/example/gateway/
│   ├── GatewayApplication.java
│   └── security/
│       ├── JwtService.java               ← extrai claims do JWT
│       └── JwtAuthenticationFilter.java  ← HandlerFilterFunction, valida e propaga headers
├── src/main/resources/
│   └── application.yml                   ← config.import
│   └── application-dev.yml               ← config.import local
├── build.gradle
├── Dockerfile
└── settings.gradle
```

---

## Decisões e Tradeoffs

### JWT centralizado no Gateway vs distribuído nos serviços

**Decisão:** validar JWT apenas no Gateway, propagar `X-User-Id` e `X-User-Roles` nos headers.

| | JWT no Gateway | JWT em cada serviço |
|---|---|---|
| Dependência jjwt | Só no Gateway | Em todos os serviços |
| Mudança na estratégia de auth | Um lugar só | Todos os serviços |
| Confiança entre serviços | Headers internos | Token revalidado |
| Segurança interna | Depende da rede | Mais seguro |
| Complexidade | Menor | Maior |

**Por que Gateway:** para portfólio e desenvolvimento local, a rede interna entre containers é confiável. Em produção real com zero-trust, cada serviço validaria o token também — mas isso seria over-engineering para o contexto atual.

---

### HandlerFilterFunction vs GatewayFilter

**Decisão:** `HandlerFilterFunction` — aplica a todos os requests automaticamente.

`GatewayFilter` precisaria ser declarado em cada rota no `gateway.yml`. `HandlerFilterFunction` com lista de `PUBLIC_PATHS` é mais simples e garante que nenhuma rota nova esquece de adicionar o filtro.

---

### Rotas públicas
```java
private static final List<String> PUBLIC_PATHS = List.of(
    "/auth/login",
    "/auth/refresh",
    "/auth/logout",
    "/actuator"
);
```

`/actuator` público permite healthcheck do Docker sem autenticação. Em produção, restringiria por IP ou removeria da lista pública.

---

### X-User-Id vs X-User-Email

**Decisão:** `X-User-Id` recebe o email (subject do JWT) por ora.

O subject do JWT é o email (`john@example.com`). Quando o `loan-service` precisar buscar o usuário pelo email no `auth-service`, já tem o valor correto no header. Quando tivermos o `auth-service` pronto, podemos avaliar se faz sentido trocar para o ID numérico.

---

## Verificação
```bash
# Gateway health
curl http://localhost:8080/actuator/health

# Eureka dashboard — GATEWAY deve aparecer registrado
http://localhost:8761
```

---

## Ordem de subida atual
```
1. postgres + redis    infraestrutura
2. config-server       porta 8888
3. eureka-server       porta 8761
4. gateway             porta 8080
```

---

## Arquivos criados/modificados

| Arquivo | Ação |
|---|---|
| `gateway/` | Criado via Spring Initializr |
| `gateway/build.gradle` | Ajustado — Spring Cloud 2025.1.1 + JWT + Lombok + actuator |
| `gateway/src/main/resources/application.yml` | Criado — substitui application.properties |
| `gateway/src/main/java/.../GatewayApplication.java` | Mantido |
| `gateway/src/main/java/.../security/JwtService.java` | Criado |
| `gateway/src/main/java/.../security/JwtAuthenticationFilter.java` | Criado |
| `gateway/Dockerfile` | Criado |
| `docker-compose.yml` | Atualizado — gateway adicionado |
| `settings.gradle` (raiz) | Modificado — `include 'gateway'` |
| `.github/dependabot.yml` | Modificado — entrada do gateway |

---

## Próxima etapa

**Etapa 6 — Auth Service**

- Criar projeto Spring Boot em `auth-service/`
- Copiar domínios `auth/`, `user/`, `refresh_token/`, `security/` do monolito
- Ler `X-User-Id` do header (sem SecurityContextHolder como fonte de User)
- Registrar no Eureka com nome `auth-service`
- Porta aleatória (`0`)
- Testes unitários e de integração (80%+ cobertura)