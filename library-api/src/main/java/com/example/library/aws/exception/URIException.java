package com.example.library.aws.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;

import com.example.library.common.exception.ApiException;

public class URIException extends ApiException {

	public URIException() {

		HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
		String title = "URL to URI error";
		String detail = "Erro ao converter URL para URI";
		URI type = URI.create("https://api.library/errors/uri-error");

		super(title, type, detail, httpStatus);
	}
}