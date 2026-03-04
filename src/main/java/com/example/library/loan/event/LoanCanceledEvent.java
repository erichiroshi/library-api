package com.example.library.loan.event;

import java.util.Map;

/**
 * Evento publicado quando um empréstimo é cancelado.
 *
 * Idêntico ao LoanReturnedEvent em estrutura — separado semanticamente
 * pois o comportamento futuro pode divergir (ex: notificações diferentes,
 * métricas separadas, regras de negócio distintas).
 */
public record LoanCanceledEvent(
        Long loanId,
        Long userId,
        Map<Long, Integer> bookQuantities   // bookId → quantidade a restaurar
) {}