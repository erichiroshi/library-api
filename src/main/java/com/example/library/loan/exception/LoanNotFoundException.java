package com.example.library.loan.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;

import com.example.library.shared.exception.ApiException;

public class LoanNotFoundException extends ApiException {

	public LoanNotFoundException(Long id) {

		super(
				"Loan Not Found",
				URI.create("https://api.library/errors/loan-not-found"),
				"Loan not found. Id: " + id,
				HttpStatus.NOT_FOUND
		);
	}
}