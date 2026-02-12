package com.example.library.shared.excpetion;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	
	@ExceptionHandler(PropertyReferenceException.class)
	public ProblemDetail handleInvalidSort(PropertyReferenceException ex) {
		
	    log.warn("Invalid sort property: {}", ex.getPropertyName());

	    return setProblemDetail(
	        HttpStatus.BAD_REQUEST,
	        "Invalid sort property",
	        "The provided sort field is invalid: " + ex.getPropertyName(),
	        URI.create("https://api.library/errors/invalid-sort")
	    );
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {

		log.warn("Validation failed for {} fields: {}", 
				ex.getErrorCount(), 
				ex.getBindingResult().getFieldErrors());

		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> errors.put(
						error.getField(), error.getDefaultMessage()));

		ProblemDetail problem = setProblemDetail(
				HttpStatus.BAD_REQUEST, 
				"Validation error", 
				"Invalid request data", 
				null);
		
		problem.setProperty("errors", errors);

		return problem;
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {

		log.warn("Database integrity violation | rootCause={}", getRootCause(ex).getClass().getSimpleName());

		return setProblemDetail(
				HttpStatus.BAD_REQUEST,
				"Database Integrity Violation",
				"Operation violates database constraints.",
				URI.create("https://api.library/errors/database-integrity"));
	}

	@ExceptionHandler(ApiException.class)
	public ProblemDetail handleApi(ApiException ex) {

		log.warn("Handled exception | status={} | type={} | detail={}", ex.getStatus().value(), ex.getType(),
				ex.getMessage());

		return setProblemDetail(
				ex.getStatus(),
				ex.getTitle(),
				ex.getDetail(), 
				ex.getType());
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpected(Exception ex) {

		log.error("Unexpected error occurred", ex);

		return setProblemDetail(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"Internal Server Error",
				"An unexpected error occurred.",
				null);

	}

	private ProblemDetail setProblemDetail(HttpStatus status, String title, String detail, URI type) {
		
		ProblemDetail pd = ProblemDetail.forStatus(status);
		
		pd.setTitle(title);
		pd.setDetail(detail);
		if (type != null) {
			pd.setType(type);
		}
		pd.setProperty("timestamp", OffsetDateTime.now());
		
		return pd;
	}

	private Throwable getRootCause(Throwable ex) {
		Throwable root = ex;
		while (root.getCause() != null) {
			root = root.getCause();
		}
		return root;
	}

}
