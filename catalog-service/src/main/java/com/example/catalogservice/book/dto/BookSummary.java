package com.example.catalogservice.book.dto;

/**
 * Contrato mínimo que o domínio Book expõe para outros domínios.
 *
 * Anticorruption Layer (ACL) — outros domínios nunca dependem da
 * entidade Book diretamente, apenas deste contrato.
 *
 * Quando Book virar um microservice, este record será preenchido
 * via HTTP (BookClient) em vez de consulta direta ao repositório,
 * sem nenhuma mudança nos consumidores.
 */
public record BookSummary(
        Long id,
        String title,
        Integer availableCopies
) {}