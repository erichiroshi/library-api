package com.example.library.refresh_token.exception;

import java.net.URI;
import java.time.Instant;

import org.springframework.http.HttpStatus;

import com.example.library.common.exception.ApiException;

public class ExpiredRefreshTokenException extends ApiException {

	public ExpiredRefreshTokenException(Instant expireDate) {

		HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
		String title = "Expired Refresh Token";
		String detail = "Expired refresh token. Expire Date: " + expireDate;
		URI type = URI.create("https://api.library/errors/expired-refresh-token");

		super(title, type, detail, httpStatus);
	}
}