package com.example.authservice.user.dto;

/**
 * Contrato mínimo que o domínio User expõe para outros domínios.
 *
 * Anticorruption Layer (ACL) — outros domínios nunca dependem da
 * entidade User diretamente, apenas deste contrato.
 *
 * Quando User/Auth virar um microservice, este record será preenchido
 * via claims do JWT ou via HTTP (UserClient), sem mudança nos consumidores.
 */
public record UserSummary(
        Long id,
        String name,
        String email
) {}