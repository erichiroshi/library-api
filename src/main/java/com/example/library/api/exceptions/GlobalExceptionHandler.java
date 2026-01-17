package com.example.library.api.exceptions;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.library.domain.exceptions.BusinessException;
import com.example.library.domain.exceptions.ResourceNotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {

		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
		problem.setTitle("Resource not found");
		problem.setDetail(ex.getMessage());
		problem.setProperty("timestamp", Instant.now());
		problem.setProperty("path", request.getRequestURI());
		problem.setType(URI.create("https://api.biblioteca/errors/not-found"));
		return problem;
	}

	@ExceptionHandler(BusinessException.class)
	public ProblemDetail handleBusiness(BusinessException ex, HttpServletRequest request) {
		
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_CONTENT);
		problem.setTitle("Business rule violation");
		problem.setDetail(ex.getMessage());
		problem.setProperty("timestamp", Instant.now());
		problem.setProperty("path", request.getRequestURI());
		return problem;
	}
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {

		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getFieldErrors()
		.forEach(error ->
		errors.put(error.getField(), error.getDefaultMessage())
				);
		
	    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation error");
        problem.setDetail("Invalid request data");
        problem.setProperty("errors", errors);
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());

	    return problem;
	}
	
	@ExceptionHandler(ConstraintViolationException.class)
	public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		problem.setTitle("Constraint violation");
		problem.setDetail(ex.getMessage());
		problem.setProperty("timestamp", Instant.now());
		problem.setProperty("path", request.getRequestURI());
		return problem;
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleGeneric(Exception ex, HttpServletRequest request) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		problem.setTitle("Internal server error");
		problem.setDetail("Unexpected error");
		problem.setProperty("timestamp", Instant.now());
		problem.setProperty("path", request.getRequestURI());
		return problem;
	}
}
