package br.com.erichiroshi.libraryapi1.api.resource.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import br.com.erichiroshi.libraryapi1.service.exception.BusinessException;

@ControllerAdvice
public class ExceptionHandler {

	@org.springframework.web.bind.annotation.ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public ResponseEntity<ApiErros> handleValidationsExceptions(MethodArgumentNotValidException ex) {
		BindingResult bindingResult = ex.getBindingResult();
		ApiErros apiErros = new ApiErros(bindingResult);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiErros);
	}

	@org.springframework.web.bind.annotation.ExceptionHandler(BusinessException.class)
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public ResponseEntity<ApiErros> handleBusinessExceptions(BusinessException ex) {
		ApiErros apiErros = new ApiErros(ex);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiErros);
	}

	@org.springframework.web.bind.annotation.ExceptionHandler(LivroNaoExisteException.class)
	@ResponseStatus(value = HttpStatus.NOT_FOUND)
	public ResponseEntity<ApiErros> handleLivroNaoEncontradoExceptions(LivroNaoExisteException ex) {
		ApiErros apiErros = new ApiErros(ex);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiErros);
	}
	
    @org.springframework.web.bind.annotation.ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErros> handleResponseStatusException( ResponseStatusException ex ){
    	ApiErros apiErros = new ApiErros(ex);
        return ResponseEntity.status(ex.getStatus().value()).body(apiErros);
    }
}
