package com.example.library.author.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;

import com.example.library.common.exception.ApiException;

public class AuthorNotFoundException extends ApiException {

	public AuthorNotFoundException(Long id) {

		HttpStatus httpStatus = HttpStatus.NOT_FOUND;
		String title = "Author Not Found";
		String detail = "Author not found. ID: " + id;
		URI type = URI.create("https://api.library/errors/author-not-found");

		super(title, type, detail, httpStatus);
	}
}