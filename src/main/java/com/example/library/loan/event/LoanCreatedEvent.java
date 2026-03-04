package com.example.library.loan.event;

import java.util.Set;

/**
 * Evento publicado quando um novo empréstimo é criado com sucesso.
 *
 * Carrega apenas os IDs necessários — sem referências a entidades JPA.
 * Isso garante que o evento seja serializável quando migrarmos para
 * mensageria (Kafka/RabbitMQ) sem nenhuma mudança no contrato.
 */
public record LoanCreatedEvent(
        Long loanId,
        Long userId,
        Set<Long> bookIds
) {}