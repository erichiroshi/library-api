package com.example.catalogservice.author.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;

import com.example.catalogservice.common.exception.ApiException;

public class AuthorNotFoundException extends ApiException {

	public AuthorNotFoundException(Long id) {

		HttpStatus httpStatus = HttpStatus.NOT_FOUND;
		String title = "Author Not Found";
		String detail = "Author not found. ID: " + id;
		URI type = URI.create("https://api.library/errors/author-not-found");

		super(title, type, detail, httpStatus);
	}
}