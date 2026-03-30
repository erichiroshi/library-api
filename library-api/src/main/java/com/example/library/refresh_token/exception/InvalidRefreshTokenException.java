package com.example.library.refresh_token.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;

import com.example.library.common.exception.ApiException;

public class InvalidRefreshTokenException extends ApiException {

	public InvalidRefreshTokenException(String token) {

		HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
		String title = "Invalid Refresh Token";
		String detail = "Invalid refresh token. Token: " + token;
		URI type = URI.create("https://api.library/errors/invalid-refresh-token");

		super(title, type, detail, httpStatus);
	}
}