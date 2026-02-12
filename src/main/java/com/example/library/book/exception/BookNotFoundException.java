package com.example.library.book.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;

import com.example.library.shared.excpetion.ApiException;

public class BookNotFoundException extends ApiException {

	public BookNotFoundException(Long id) {

		HttpStatus httpStatus = HttpStatus.NOT_FOUND;
		String title = "Book Not Found";
		String detail = "Book not found. ID: " + id;
		URI type = URI.create("https://api.library/errors/book-not-found");

		super(title, type, detail, httpStatus);
	}

}
