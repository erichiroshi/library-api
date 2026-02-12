package com.example.library.book.exception;

import java.net.URI;
import java.util.Set;

import org.springframework.http.HttpStatus;

import com.example.library.shared.excpetion.ApiException;

public class InvalidOperationException extends ApiException {

	public InvalidOperationException() {
		HttpStatus status = HttpStatus.NOT_FOUND;
		String title = "Author Not Found";
		String detail = "At least one author must be informed";
		URI type = URI.create("https://api.library/errors/author-not-found");
		super(title, type, detail, status);
	}

	public InvalidOperationException(Set<Long> ids) {

		HttpStatus status = HttpStatus.NOT_FOUND;
		String title = "Author Not Found";
		String detail = "Some authors were not found. ID's: " + ids;
		URI type = URI.create("https://api.library/errors/author-not-found");

		super(title, type, detail, status);
	}

}
