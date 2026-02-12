package com.example.library.book.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;

import com.example.library.shared.excpetion.ApiException;

public class BookAlreadyExistsException extends ApiException {

	public BookAlreadyExistsException(String isbn) {

		HttpStatus httpStatus = HttpStatus.NOT_FOUND;
		String title = "Book Already Exists";
		String detail = "Book Already Exists. ISBN: " + isbn;
		URI type = URI.create("https://api.library/errors/book-already-exists");

		super(title, type, detail, httpStatus);
	}

}
