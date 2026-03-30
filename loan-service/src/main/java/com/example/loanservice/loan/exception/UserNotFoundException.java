package com.example.loanservice.loan.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;

import com.example.loanservice.common.exception.ApiException;

public class UserNotFoundException extends ApiException {

	public UserNotFoundException(Long id) {

		HttpStatus httpStatus = HttpStatus.NOT_FOUND;
		String title = "User Not Found";
		String detail = "User not found. ID: " + id;
		URI type = URI.create("https://api.library/errors/user-not-found");

		super(title, type, detail, httpStatus);
	}

	public UserNotFoundException(String email) {

		HttpStatus httpStatus = HttpStatus.NOT_FOUND;
		String title = "User Not Found";
		String detail = "User not found. Email: " + email;
		URI type = URI.create("https://api.library/errors/user-not-found");

		super(title, type, detail, httpStatus);
	}

}
