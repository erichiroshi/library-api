package com.example.library.aws.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;

import com.example.library.common.exception.ApiException;

public class AmazonClientException extends ApiException {

	public AmazonClientException() {

		HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
		String title = "Erro Amazon Client";
		String detail = "The AWS Access Key Id you provided does not exist in our records.";
		URI type = URI.create("https://api.library/errors/amazon-client-error");

		super(title, type, detail, httpStatus);
	}

}
