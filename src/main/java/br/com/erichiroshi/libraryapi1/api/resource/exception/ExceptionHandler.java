package br.com.erichiroshi.libraryapi1.api.resource.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;

import br.com.erichiroshi.libraryapi1.service.exception.BusinessException;

@ControllerAdvice
public class ExceptionHandler {

	@org.springframework.web.bind.annotation.ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public ResponseEntity<ApiErrors> handleValidationsExceptions(MethodArgumentNotValidException ex) {
		BindingResult bindingResult = ex.getBindingResult();
		ApiErrors apiErrors = new ApiErrors(bindingResult);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiErrors);
	}
	
	@org.springframework.web.bind.annotation.ExceptionHandler(BusinessException.class)
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public ResponseEntity<ApiErrors> handleBusinessExceptions(BusinessException ex) {
		ApiErrors apiErrors = new ApiErrors(ex);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiErrors);
	}
}
