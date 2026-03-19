package com.example.library.loan.dto;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;

/**
 * DTO para criação de empréstimo.
 */
public record LoanCreateDTO(

        @NotEmpty(message = "At least one book must be informed")
        Set<Long> booksId

) {}