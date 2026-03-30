# JWT + Segurança — Library API

## O que este documento cobre

Como a autenticação e autorização foram implementadas no projeto: JWT sem query ao banco, Refresh Token Rotation, separação de responsabilidades entre filter e service, profiles de segurança dev/prod, e como tudo isso muda na transição para microservices com Gateway centralizado.

---

## Visão Geral do Fluxo de Autenticação

```
1. POST /auth/login
   → AuthController autentica via AuthenticationManager
   → Gera access token (JWT) + refresh token (UUID no banco)
   → Retorna ambos ao cliente

2. Requisições subsequentes
   → Cliente envia: Authorization: Bearer <access_token>
   → JwtAuthenticationFilter intercepta
   → Valida JWT, extrai username + roles do token
   → Popula SecurityContext — sem query ao banco
   → Request segue para o Controller

3. Access token expirado
   → Cliente envia refresh token para POST /auth/refresh
   → Sistema valida refresh token no banco
   → Gera novo access token + novo refresh token (rotation)
   → Invalida o refresh token antigo
```

---

## JWT: O que é e por que é stateless

JWT (JSON Web Token) é um token autocontido — todas as informações necessárias para autenticar o usuário estão dentro do próprio token, assinadas criptograficamente.

Estrutura de um JWT:

```
header.payload.signature

header:    { "alg": "HS256" }
payload:   { "sub": "joao@email.com", "roles": ["ROLE_USER"], "exp": 1234567890 }
signature: HMAC-SHA256(header + payload, secret_key)
```

O servidor não precisa armazenar sessões. Para validar um JWT, basta verificar a assinatura com a chave secreta — se bater, o token é legítimo. O payload é lido diretamente.

### Trade-off fundamental do JWT

**Vantagem**: stateless, escalável horizontalmente. Qualquer instância valida qualquer token sem sincronização.

**Desvantagem**: não é possível invalidar um token antes do vencimento. Se um access token vazar, ele permanece válido até expirar. Por isso o access token tem duração curta (15 minutos em prod, 30 em dev), e o mecanismo de logout invalida o refresh token — não o access token.

---

## JwtService: Geração e Validação

O `JwtService` centraliza toda a lógica de JWT:

```java
// Gera token com subject (email) e roles no payload
public String generateToken(UserDetails user) {
    return Jwts.builder()
        .subject(user.getUsername())
        .claim("roles", user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList())
        .issuedAt(new Date())
        .expiration(Date.from(Instant.now().plus(accessTokenSeconds, ChronoUnit.SECONDS)))
        .signWith(key)
        .compact();
}

// Extrai roles diretamente do token — sem query ao banco
public List<String> extractRoles(String token) {
    Claims claims = parseClaims(token);
    Object roles = claims.get("roles");
    if (roles instanceof List<?> list) {
        return list.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }
    return List.of();
}
```

A chave secreta é construída uma vez no construtor e reutilizada:

```java
public JwtService(@Value("${jwt.secret-key}") String secret, Environment environment) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
}
```

### Validação da chave em produção

Um `@PostConstruct` garante que a aplicação não sobe com uma chave insegura em prod:

```java
@PostConstruct
public void validate() {
    if (secret.contains("dev-insecure") && !isDevelopmentProfile()) {
        throw new IllegalStateException("Insecure JWT key in non-dev environment!");
    }
}
```

Isso é uma camada de proteção contra o erro comum de esquecer de configurar a chave secreta no ambiente de produção.

---

## JwtAuthenticationFilter: A Decisão Central

O filter é onde mora a decisão técnica mais relevante da segurança do projeto.

### O problema que existia antes

A implementação comum do JWT com Spring Security faz o seguinte:

```
Request → Filter → parseia JWT → pega username → chama userDetailsService.loadUserByUsername()
                                                         ↓
                                                   SELECT * FROM tb_user WHERE email = ?
                                                         ↓
                                                   popula SecurityContext
```

Isso significa **uma query ao banco em cada request autenticado**. Para uma API com muitas requisições, isso é um gargalo desnecessário — as informações que o Spring Security precisa (username e roles) já estão no JWT.

### A solução implementada

```java
@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain) throws ServletException, IOException {

    String header = request.getHeader("Authorization");

    if (header != null && header.startsWith("Bearer ")) {
        String token = header.substring(7);

        if (jwtService.isTokenValid(token)) {
            String username = jwtService.extractUsername(token);

            // Roles lidas do JWT — zero queries ao banco
            List<SimpleGrantedAuthority> authorities = jwtService.extractRoles(token)
                    .stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }

    filterChain.doFilter(request, response);
}
```

O `Authentication` é construído diretamente com username e roles do token. O `UserDetailsService` não é chamado.

### Quando o banco SIM é consultado

O único momento em que o banco é consultado para autenticação é no login:

```
POST /auth/login → AuthenticationManager → UserDetailsService.loadUserByUsername()
                                                ↓
                                          SELECT tb_user WHERE email = ?
                                                ↓
                                          AuthController gera JWT com roles
```

A partir daí, o JWT carrega tudo que é necessário. O banco só volta a ser consultado quando o serviço precisa de dados completos do usuário para lógica de negócio (ex: `LoanService` precisa do objeto `User` para criar um empréstimo — aí sim faz query).

### Trade-off

**Vantagem**: elimina 1 query por request autenticado. Em alta carga, isso é significativo.

**Desvantagem**: se as roles de um usuário mudarem (ex: ADMIN revogado), o access token em circulação continua com as roles antigas até expirar. Solução: access token de curta duração (15 min) minimiza essa janela.

---

## Refresh Token Rotation

O refresh token resolve o problema de UX do access token de curta duração: sem ele, o usuário precisaria fazer login a cada 15 minutos.

### Como funciona

```
Cliente                           Servidor
  │                                  |
  │── POST /auth/login ─────────────▶│
  │                                  │ gera access_token (15 min)
  │                                  │ gera refresh_token (7 dias) → salva no banco
  │◀─ { access_token, refresh_token }│
  │                                  │
  │  ... 15 minutos depois ...       │
  │                                  │
  │── POST /auth/refresh ───────────▶│
  │   { refresh_token: "abc..." }    │ busca no banco — válido?
  │                                  │ deleta refresh_token antigo
  │                                  │ gera novo access_token
  │                                  │ gera novo refresh_token → salva
  │◀─ { novo access_token,           │
  │     novo refresh_token }         │
```

### Por que rotation?

Sem rotation, o refresh token é estático — se vazar, o atacante tem acesso indefinido até o token expirar (7 dias). Com rotation, cada uso do refresh token gera um novo. Se o token original vazar e o atacante tentar usá-lo depois que o usuário legítimo já o usou, o token não existirá mais no banco — o ataque falha.

### Implementação

```java
public RefreshToken create(User user) {
    // Remove token antigo antes de criar novo
    repository.findByUser(user).ifPresent(repository::delete);

    RefreshToken refresh = RefreshToken.builder()
            .token(UUID.randomUUID().toString() + UUID.randomUUID()) // ~72 chars
            .expiryDate(Instant.now().plus(Duration.ofDays(durationRefreshToken)))
            .user(user)
            .build();

    return repository.save(refresh);
}

public RefreshToken validate(String token) {
    RefreshToken refresh = repository.findByToken(token)
            .orElseThrow(() -> new InvalidRefreshTokenException(token));

    if (refresh.getExpiryDate().isBefore(Instant.now())) {
        repository.delete(refresh);  // limpa token expirado ao tentar usar
        throw new ExpiredRefreshTokenException(refresh.getExpiryDate());
    }

    return refresh;
}
```

### Logout

O logout invalida o refresh token no banco. O access token em circulação permanece válido até expirar — isso é uma limitação conhecida do JWT stateless. A janela de risco é a duração do access token (15 min em prod).

```java
public void invalidate(String refreshToken) {
    RefreshToken refresh = validate(refreshToken);
    repository.delete(refresh);
}
```

---

## Limpeza de Tokens Expirados

Tokens que nunca são reutilizados (usuário não faz logout, simplesmente abandona) acumulam no banco indefinidamente. O `RefreshTokenCleanupJob` roda toda madrugada para limpar:

```java
@Scheduled(cron = "0 0 2 * * *")
@SchedulerLock(name = "refreshTokenCleanupJob", lockAtLeastFor = "30m", lockAtMostFor = "1h")
public void cleanupExpiredTokens() {
    cleanupService.deleteExpiredTokens();
}
```

A separação entre Job e Service é intencional: `@SchedulerLock` não pode coexistir com `@Transactional` no mesmo método — o ShedLock precisa adquirir e liberar o lock **fora** da transação para garantir que a proteção distribuída funcione corretamente. O lock no banco é liberado apenas quando o método retorna, não quando a transação comita.

---

## Autorização: Roles e @PreAuthorize

As roles são armazenadas como strings na tabela `tb_user_roles` e incluídas no JWT:

```
ROLE_ADMIN → acesso total
ROLE_USER  → acesso restrito
```

A autorização por endpoint usa `@PreAuthorize`:

```java
@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Void> deleteById(@PathVariable Long id) { ... }
```

Para recursos onde o acesso depende de quem é o dono (empréstimo), a validação é feita manualmente no service:

```java
private void validateOwnershipOrAdmin(Loan loan, User user) {
    boolean isAdmin = user.getRoles().contains("ROLE_ADMIN");
    boolean isOwner = loan.getUser().getId().equals(user.getId());

    if (!isOwner && !isAdmin) {
        throw new LoanUnauthorizedException(loan.getId());
    }
}
```

### Por que LoanUnauthorizedException retorna 404?

Se retornasse 403 (Forbidden), o cliente saberia que o empréstimo existe mas ele não tem acesso. Retornar 404 (Not Found) não revela a existência do recurso — princípio de segurança por obscuridade aplicado onde faz sentido.

---

## Profiles de Segurança: Dev vs Prod

Dois `SecurityFilterChain` separados por profile garantem comportamentos distintos:

**Dev** (`ResourceSecurityDev`):
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/auth/**", "/actuator/**",
                     "/swagger-ui/**", "/v3/api-docs/**").permitAll()
    .anyRequest().authenticated()
)
```

**Prod** (`ResourceSecurityProd`):
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/auth/**").permitAll()
    .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
    .anyRequest().authenticated()
)
```

Diferenças relevantes:
- Swagger exposto apenas em dev
- Actuator completo em dev, apenas `health` e `prometheus` em prod
- Access token: 30 min em dev, 15 min em prod
- Refresh token: 14 dias em dev, 7 dias em prod
- Chave JWT: pode usar key insegura em dev, obrigatória via env var em prod

### Por que dois beans em vez de condicionais no código?

Condicional no código (`if (isDev)`) mistura responsabilidades e é difícil de testar. Dois `@Configuration` com `@Profile` são autoexplicativos, independentes e seguros — o Spring instancia apenas um dependendo do profile ativo.

---

## Segurança na Fase 3: Gateway Centralizado

Na transição para microservices, a responsabilidade de validar JWT sai de cada serviço e vai para o Gateway.

### Por que centralizar no Gateway?

Com múltiplos serviços, cada um teria que:
- Conhecer a chave secreta JWT
- Implementar e manter o filter
- Lidar com expiração, roles, extração de claims

Centralizar no Gateway significa um único ponto de validação. Os serviços recebem as informações já processadas via headers.

### O fluxo na Fase 3

```
Cliente
  │
  ▼
Gateway (porta 8080)
  │
  ├── JwtAuthenticationFilter (igual ao atual, mas só no Gateway)
  │     → valida JWT
  │     → extrai userId e roles
  │     → propaga como headers:
  │         X-User-Id: 42
  │         X-User-Roles: ROLE_USER,ROLE_ADMIN
  │
  ├── /auth/**        → auth-service   (rota livre, sem filtro JWT)
  ├── /api/v1/books** → catalog-service
  └── /api/v1/loans** → loan-service

catalog-service / loan-service:
  → leem X-User-Id e X-User-Roles dos headers
  → confiam no Gateway — não revalidam JWT
  → nunca precisam da chave secreta JWT
```

### O que muda nos serviços

Hoje o `LoanService` busca o usuário autenticado assim:

```java
private User getAuthenticatedUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String email = (String) auth.getPrincipal();
    return userLookupService.findByEmail(email)  // query ao banco
            .orElseThrow(...);
}
```

No microservice, o `loan-service` leria diretamente do header:

```java
// Sem query ao banco — userId vem do header propagado pelo Gateway
Long userId = Long.parseLong(request.getHeader("X-User-Id"));
```

Isso elimina a dependência do `loan-service` em relação ao `auth-service` para operações simples — o userId já está disponível no header.

### Trade-off do Gateway centralizado

**Vantagem**: lógica de segurança em um único lugar, serviços mais simples, chave JWT só o Gateway conhece.

**Desvantagem**: o Gateway vira ponto único de falha para autenticação. Precisa ser robusto, com alta disponibilidade e sem estado (stateless) para escalar horizontalmente. Se o Gateway cair, nenhum serviço autenticado responde.

---

## Resumo das Decisões de Segurança

| Decisão | Alternativa | Por que esta |
|---|---|---|
| JWT sem query ao banco no filter | UserDetailsService a cada request | Elimina 1 query por request autenticado |
| Roles no payload do JWT | Consultar banco para verificar roles | Performance + stateless |
| Refresh Token Rotation | Refresh token estático | Token vazado fica inútil após primeiro uso legítimo |
| Access token curto (15 min) | Token longo (horas/dias) | Minimiza janela de risco se token vazar |
| Dois SecurityFilterChain por profile | Condicionais no código | Clareza, testabilidade, separação de responsabilidade |
| LoanUnauthorizedException → 404 | 403 Forbidden | Não revela existência do recurso |
| Chave JWT validada no @PostConstruct | Falhar em runtime | Fail fast — erro na inicialização, não em produção |
| Gateway centraliza JWT na Fase 3 | Cada serviço valida JWT | Lógica de segurança em um único lugar |
