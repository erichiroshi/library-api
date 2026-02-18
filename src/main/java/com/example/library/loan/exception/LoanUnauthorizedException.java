package com.example.library.loan.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;

import com.example.library.shared.exception.ApiException;

/**
 * Lançada quando um usuário tenta acessar um empréstimo que não é seu.
 * Retorna 404 intencionalmente — não deve revelar que o loan existe.
 */
public class LoanUnauthorizedException extends ApiException {

    public LoanUnauthorizedException(Long loanId) {
        super(
            "Loan Not Found",
            URI.create("https://api.library/errors/loan-not-found"),
            "Loan not found. Id: " + loanId,
            HttpStatus.NOT_FOUND
        );
    }
}