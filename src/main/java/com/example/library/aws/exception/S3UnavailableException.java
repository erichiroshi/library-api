package com.example.library.aws.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;

import com.example.library.common.exception.ApiException;

public class S3UnavailableException extends ApiException {

	public S3UnavailableException(String detail) {

		HttpStatus httpStatus = HttpStatus.SERVICE_UNAVAILABLE;
		String title = "Erro Amazon S3";
		URI type = URI.create("https://api.library/errors/amazon-S3-error");

		super(title, type, detail, httpStatus);
	}

}
