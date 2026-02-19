package com.example.library.common.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public abstract class ApiException extends RuntimeException {

	private final String title;
	private final URI type;
	private final String detail;
	private final HttpStatus status;

	protected ApiException(String title, URI type, String detail, HttpStatus status) {
		super(detail);
		this.title = title;
		this.type = type;
		this.detail = detail;
		this.status = status;
	}

}
