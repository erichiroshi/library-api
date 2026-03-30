# AWS S3 + BookMediaService — Library API

## O que este documento cobre

Como o upload e gerenciamento de imagens de capa de livros foi implementado: a separação entre `BookService` e `BookMediaService`, o fluxo de upload direto vs presigned URL, a integração com o S3, o Circuit Breaker protegendo a dependência externa, e os trade-offs de cada decisão.

---

## O Problema

Livros têm capas. Capas são imagens. Imagens não pertencem ao banco de dados relacional.

Armazenar imagens como `BYTEA` no PostgreSQL é possível, mas problemático:
- Queries de livros carregariam bytes de imagem desnecessariamente
- Backup do banco ficaria gigantesco
- Escalabilidade de storage acoplada ao banco
- Sem CDN nativo para servir as imagens

A solução padrão é armazenar as imagens em um serviço de object storage (S3) e guardar apenas a URL no banco.

---

## A Separação BookService / BookMediaService

Esta é uma das decisões arquiteturais mais deliberadas do projeto: `BookMediaService` é um serviço completamente separado de `BookService`.

### Por que separar?

`BookService` tem responsabilidades bem definidas: CRUD de livros, validações de negócio, cache. Misturar lógica de upload S3 em `BookService` criaria um serviço com duas razões para mudar — mudanças de negócio em livros E mudanças na estratégia de armazenamento de mídia.

Separar segue o **Single Responsibility Principle** no nível de serviço:

```
BookService       → o que um livro é (dados, regras de negócio)
BookMediaService  → como a mídia de um livro é armazenada
```

### Consequência prática

Se no futuro a estratégia mudar de S3 para Google Cloud Storage, ou se for adicionado processamento de imagem (resize, thumbnail), apenas `BookMediaService` muda. `BookService` não é tocado.

---

## Fluxo de Upload

```
Cliente
  │
  ├─── POST /api/v1/books/{id}/cover
  │         multipart/form-data (arquivo de imagem)
  │
  └──── BookMediaController
              │
              └── BookMediaService
                      │
                      ├── valida tipo e tamanho do arquivo
                      │
                      ├── gera chave única: covers/{bookId}/{uuid}.{ext}
                      │
                      ├── S3Client.putObject() ──→ AWS S3
                      │
                      ├── gera URL pública: https://bucket.s3.region.amazonaws.com/{key}
                      │
                      └── BookService.updateCoverImageUrl(bookId, url)
                                │
                                └── UPDATE books SET cover_image_url = ? WHERE id = ?
                                    + @CacheEvict(bookById, books)
```

### Geração da chave

```java
String key = "covers/" + bookId + "/" + UUID.randomUUID() + "." + extension;
```

O UUID garante unicidade — mesmo que o mesmo livro tenha o cover atualizado várias vezes, cada upload gera uma chave diferente. Isso evita problemas de cache do browser com a mesma URL retornando imagens diferentes.

### Validação de arquivo

```java
// tipos permitidos
private static final Set<String> ALLOWED_TYPES = Set.of(
    "image/jpeg", "image/png", "image/webp"
);

// tamanho máximo: 5MB
private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024;
```

Validações no serviço, não apenas no controller — a regra de negócio fica no lugar correto.

---

## Configuração do S3

```yaml
aws:
  access-key-id: ${AWS_KEY}
  secret-access-key: ${AWS_SECRET}
s3:
  bucket: ${BUCKET_NAME:library-api-s3}
  region: ${BUCKET_REGION:sa-east-1}
```

Todas as configurações sensíveis vêm de variáveis de ambiente — nunca hard-coded, nunca no repositório.

### Configuração

```java
// S3Config.java
@Bean
S3Client s3client() {

  AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider
      .create(AwsBasicCredentials.create(awsId, awsKey));
  
  return S3Client
      .builder()
      .region(Region.of(region))
      .credentialsProvider(credentialsProvider)
      .build();
}
```

---

## Circuit Breaker no S3

O S3 é uma dependência externa. Dependências externas falham — timeout, erro de rede, instabilidade da AWS. Sem proteção, uma falha no S3 poderia cascatear para a aplicação inteira.

O Circuit Breaker (via Resilience4j) protege as chamadas ao S3:

```java
@CircuitBreaker(name = "s3", fallbackMethod = "uploadFallback")
public String uploadCover(Long bookId, MultipartFile file) {
    // lógica de upload
    return generatePublicUrl(key);
}

private String uploadFallback(Long bookId, MultipartFile file, Exception ex) {
    log.warn("S3 unavailable for bookId={}. Circuit breaker activated.", bookId, ex);
    throw new S3UnavailableException("Cover upload temporarily unavailable.");
}
```

### Como funciona o Circuit Breaker aqui

```
Estado CLOSED (normal):
  Upload → S3 → sucesso

Após N falhas consecutivas:
  Estado muda para OPEN

Estado OPEN:
  Upload → rejeição imediata (nem tenta o S3) → fallback
  
Após período de espera:
  Estado muda para HALF-OPEN
  Tenta algumas requisições de teste
  
Se passarem:
  Estado volta para CLOSED
```

O fallback lança `S3UnavailableException`, que retorna `503 Service Unavailable` para o cliente — uma resposta clara e controlada em vez de um timeout de 30 segundos ou um `500 Internal Server Error` genérico.

### Por que não Retry aqui?

Upload de arquivo é uma operação diferente de uma query ao banco. Fazer retry de um upload significa:
- Reenviar o arquivo inteiro novamente
- Potencial de uploads duplicados se a primeira tentativa teve sucesso parcial
- Latência percebida pelo usuário aumenta drasticamente

Para uploads, Circuit Breaker sem retry é a escolha correta. Retry faz mais sentido em operações idempotentes e leves — como consultas ao banco ou chamadas de leitura a APIs externas.

---

## Deleção de Imagem

Quando um livro é deletado, a imagem no S3 deve ser removida também — caso contrário, o S3 acumula arquivos órfãos indefinidamente.

```java
@CircuitBreaker(name = "s3", fallbackMethod = "deleteFallback")
public void deleteCover(String coverImageUrl) {
    if (coverImageUrl == null || coverImageUrl.isBlank()) return;

    String key = extractKeyFromUrl(coverImageUrl);
    s3Client.deleteObject(req -> req.bucket(bucket).key(key));
}

private void deleteFallback(String coverImageUrl, Exception ex) {
    // log para limpeza manual posterior — não falha o delete do livro
    log.error("Failed to delete S3 object: {}. Manual cleanup required.", coverImageUrl, ex);
}
```

### Decisão de fallback diferente

O fallback do delete tem comportamento diferente do fallback do upload: em vez de lançar exceção, apenas loga. Isso é intencional — a deleção do livro no banco não deve falhar por causa de uma falha no S3. O livro é deletado do banco, e o arquivo órfão no S3 pode ser limpo posteriormente (via job ou manualmente).

A alternativa seria usar transações distribuídas para garantir atomicidade entre banco e S3 — mas transações distribuídas são complexas e o custo não justifica o benefício para este caso. Um arquivo órfão no S3 é um problema de storage, não um problema de integridade de dados.

---

## URL Pública vs Presigned URL

No projeto, as imagens são servidas via URL pública — o bucket é configurado com acesso público de leitura:

```
https://library-api-bucket.s3.us-east-1.amazonaws.com/covers/1/abc123.jpg
```

### Alternativa: Presigned URL

Presigned URL é uma URL temporária gerada com credenciais da AWS, que expira após um período:

```
https://bucket.s3.amazonaws.com/covers/1/abc123.jpg?X-Amz-Algorithm=...&X-Amz-Expires=3600
```

**Vantagem**: controle de acesso — apenas quem tem a URL pode acessar, e ela expira. Útil para conteúdo privado.

**Desvantagem**: complexidade — a URL precisa ser gerada dinamicamente a cada requisição, não pode ser cacheada indefinidamente pelo cliente, e a aplicação precisa assinar cada URL.

### Por que URL pública no projeto

Capas de livros são conteúdo público — não há razão para restringir acesso. URL pública é mais simples, pode ser cacheada pelo browser e por CDNs, e elimina a necessidade de gerar URLs dinamicamente. Para um sistema de biblioteca, onde as capas são inerentemente públicas, URL pública é a escolha correta.

Se o sistema tivesse documentos privados (contratos de usuários, laudos, etc.), presigned URL seria o caminho.

---

## Integração com BookService

`BookMediaService` não tem acesso direto ao `BookRepository` — acessa os dados de livros através de `BookService`. Isso mantém o encapsulamento: a lógica de persistência de livros pertence ao `BookService`.

```java
// BookMediaService
public String uploadCover(Long bookId, MultipartFile file) {
    // verifica se o livro existe (delega para BookService)
    bookService.findById(bookId);  // lança NotFoundException se não existir

    String key = generateKey(bookId, file);
    uploadToS3(key, file);
    String url = generatePublicUrl(key);

    // delega persistência da URL para BookService
    bookService.updateCoverImageUrl(bookId, url);

    return url;
}
```

`updateCoverImageUrl` em `BookService` invalida o cache — `BookMediaService` não precisa conhecer a estratégia de cache. Cada serviço tem seu domínio.

---

## Fase 3: S3 nos Microservices

`BookMediaService` migra integralmente para o `catalog-service` — faz sentido, pois capas são parte do contexto de catálogo.

A configuração do S3 (credenciais, bucket, região) vira config centralizada no **Spring Cloud Config**:

```yaml
# config-repo/catalog-service.yml
aws:
  s3:
    bucket: library-prod-catalog-bucket
    region: us-east-1
```

As credenciais (`access-key`, `secret-key`) continuam como variáveis de ambiente — nunca no repositório, nem no config-server.

### ECS/EKS: IAM Role em vez de credenciais

Em produção real na AWS (ECS ou EKS), a prática recomendada é usar **IAM Role** atribuída ao container, eliminando a necessidade de gerenciar `access-key` e `secret-key` explicitamente. O SDK da AWS detecta automaticamente as credenciais do ambiente.

---

## Resumo das Decisões

| Decisão | Alternativa | Por que esta |
|---|---|---|
| BookMediaService separado de BookService | Um único BookService | SRP: razões de mudança diferentes para dados vs mídia |
| S3 para armazenamento | BYTEA no PostgreSQL | Storage independente do banco; CDN nativo; escalabilidade |
| Circuit Breaker sem retry no upload | Retry no upload | Upload não é idempotente; retry de arquivo = latência e risco de duplicata |
| Fallback silencioso na deleção | Falha na deleção | Arquivo órfão no S3 é menos grave que falha na deleção do livro |
| URL pública | Presigned URL | Capas são conteúdo público; simplicidade; cacheável por CDN |
| UUID na chave do S3 | Nome original do arquivo | Unicidade garantida; sem colisões; sem exposição do nome original |
| BookMediaService acessa BookService | Acesso direto ao BookRepository | Encapsulamento; cache gerenciado pelo dono do dado |
