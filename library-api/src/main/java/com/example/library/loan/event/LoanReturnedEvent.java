package com.example.library.loan.event;

import java.util.Map;

/**
 * Evento publicado quando um empréstimo é devolvido.
 *
 * Carrega um mapa de bookId → quantidade devolvida, permitindo que
 * o Book domain restaure as cópias disponíveis de forma precisa.
 */
public record LoanReturnedEvent(
        Long loanId,
        Long userId,
        Map<Long, Integer> bookQuantities   // bookId → quantidade a restaurar
) {}