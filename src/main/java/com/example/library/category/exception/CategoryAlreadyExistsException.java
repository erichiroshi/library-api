package com.example.library.category.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;

import com.example.library.shared.exception.ApiException;

public class CategoryAlreadyExistsException extends ApiException {

	public CategoryAlreadyExistsException(String detail) {

		final HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
		final String title = "Category Already Exists";
		detail = "Category Already Exists: " + detail;
		final URI type = URI.create("https://api.library/errors/resource-already-exists");

		super(title, type, detail, httpStatus);
	}

}
