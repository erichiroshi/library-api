package com.example.library.loan.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;

import com.example.library.shared.exception.ApiException;

public class LoanAlreadyReturnedException extends ApiException {

    public LoanAlreadyReturnedException(Long id) {
        super(
            "Loan Already Returned",
            URI.create("https://api.library/errors/loan-already-returned"),
            "Loan already returned or canceled. Id: " + id,
            HttpStatus.CONFLICT
        );
    }
}