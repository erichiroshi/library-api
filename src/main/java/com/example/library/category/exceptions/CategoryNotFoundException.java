package com.example.library.category.exceptions;

import java.net.URI;

import org.springframework.http.HttpStatus;

import com.example.library.exceptions.ApiException;

public class CategoryNotFoundException extends ApiException {

	public CategoryNotFoundException(Long id) {

		HttpStatus httpStatus = HttpStatus.NOT_FOUND;
		String title = "Category Not Found";
		String detail = "Category not found. ID: " + id;
		URI type = URI.create("https://api.library/errors/category-not-found");

		super(title, type, detail, httpStatus);
	}

}
