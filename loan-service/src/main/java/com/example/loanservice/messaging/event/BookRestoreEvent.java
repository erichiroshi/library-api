package com.example.loanservice.messaging.event;

import java.util.Map;

/**
 * Evento publicado pelo loan-service quando um empréstimo é devolvido ou cancelado.
 * O catalog-service consome e restaura as cópias disponíveis.
 */
public record BookRestoreEvent(
        Long loanId,
        Map<Long, Integer> bookQuantities   // bookId → quantidade a restaurar
) {}