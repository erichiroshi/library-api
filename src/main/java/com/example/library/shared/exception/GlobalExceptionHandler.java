package com.example.library.shared.exception;

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
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	
	// ─────────────────────────────────────────────
	// SEGURANÇA
	// ─────────────────────────────────────────────
	
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ProblemDetail handleAuthorizationDenied(AuthorizationDeniedException ex) {

        log.warn("Access denied | reason={}", ex.getMessage());

        return setProblemDetail(
                HttpStatus.FORBIDDEN,
                "Access Denied",
                "You do not have permission to perform this action.",
                URI.create("https://api.library/errors/access-denied")
        );
    }
    
    // ─────────────────────────────────────────────
    // DOMÍNIO
    // ─────────────────────────────────────────────
    
	@ExceptionHandler(ApiException.class)
	public ProblemDetail handleApi(ApiException ex) {

		log.warn("Handled exception | status={} | type={} | detail={}",
				ex.getStatus().value(), ex.getType(), ex.getMessage());

		return setProblemDetail(
				ex.getStatus(),
				ex.getTitle(),
				ex.getDetail(), 
				ex.getType());
	}
	
    // ─────────────────────────────────────────────
    // VALIDAÇÃO
    // ────────────────────────────────────────────
    
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {

		log.warn("Validation failed for {} fields: {}", 
				ex.getErrorCount(), 
				ex.getBindingResult().getFieldErrors());

		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

		ProblemDetail problem = setProblemDetail(
				HttpStatus.BAD_REQUEST, 
				"Validation Error", 
                "One or more fields are invalid. See 'errors' for details.",
                URI.create("https://api.library/errors/validation")
        );
		problem.setProperty("errors", errors);

		return problem;
	}

	@ExceptionHandler(PropertyReferenceException.class)
	public ProblemDetail handleInvalidSort(PropertyReferenceException ex) {
		
        log.warn("Invalid sort field | field={}", ex.getPropertyName());

	    return setProblemDetail(
	    		HttpStatus.BAD_REQUEST,
	            "Invalid Sort Field",
	            "The sort field '" + ex.getPropertyName() + "' does not exist.",
	            URI.create("https://api.library/errors/invalid-sort")
	    );
	}


    // ─────────────────────────────────────────────
    // BANCO DE DADOS
    // ─────────────────────────────────────────────
	
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {

		log.warn("Database integrity violation | rootCause={}",
				getRootCause(ex).getClass().getSimpleName());

       return setProblemDetail(
                HttpStatus.CONFLICT,
                "Data Integrity Violation",
                "The operation conflicts with existing data constraints.",
                URI.create("https://api.library/errors/data-integrity")
        );
	}
	
    // ─────────────────────────────────────────────
    // FALLBACK
    // ─────────────────────────────────────────────
	
	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpected(Exception ex) {

        log.error("Unexpected error | type={} | message={}",
                ex.getClass().getSimpleName(), ex.getMessage(), ex);
        
        return setProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                null
        );
	}
	
    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

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
