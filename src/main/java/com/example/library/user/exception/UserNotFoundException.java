package com.example.library.user.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;

import com.example.library.common.exception.ApiException;

public class UserNotFoundException extends ApiException {

	public UserNotFoundException(Long id) {

		HttpStatus httpStatus = HttpStatus.NOT_FOUND;
		String title = "User Not Found";
		String detail = "User not found. ID: " + id;
		URI type = URI.create("https://api.library/errors/user-not-found");

		super(title, type, detail, httpStatus);
	}

}
