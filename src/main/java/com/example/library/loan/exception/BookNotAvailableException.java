package com.example.library.loan.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;

import com.example.library.shared.exception.ApiException;

public class BookNotAvailableException extends ApiException {

	public BookNotAvailableException(Long bookId, String bookTitle) {
		super(
				"Book Not Available",
				URI.create("https://api.library/errors/book-not-available"),
				String.format("Book '%s' (id: %d) has no available copies.", bookTitle, bookId),
				HttpStatus.CONFLICT
		);
	}
}