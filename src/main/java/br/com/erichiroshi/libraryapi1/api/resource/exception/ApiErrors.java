package br.com.erichiroshi.libraryapi1.api.resource.exception;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.validation.BindingResult;

import br.com.erichiroshi.libraryapi1.service.exception.BusinessException;
import lombok.Getter;

@Getter
public class ApiErrors {

	List<String> errors = new ArrayList<>();

	public ApiErrors(BindingResult bindingResult) {
		bindingResult.getAllErrors().forEach(error -> this.errors.add(error.getDefaultMessage()));
	}

	public ApiErrors(BusinessException ex) {
		this.errors = Arrays.asList(ex.getMessage());
	}

	public ApiErrors(LivroNaoExisteException ex) {
		this.errors = Arrays.asList(ex.getMessage());
	}

}
