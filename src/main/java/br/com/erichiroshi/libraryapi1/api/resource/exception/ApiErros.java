package br.com.erichiroshi.libraryapi1.api.resource.exception;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;

import br.com.erichiroshi.libraryapi1.service.exception.BusinessException;
import lombok.Getter;

@Getter
public class ApiErros {

	List<String> errors = new ArrayList<>();

	public ApiErros(BindingResult bindingResult) {
		bindingResult.getAllErrors().forEach(error -> this.errors.add(error.getDefaultMessage()));
	}

	public ApiErros(BusinessException ex) {
		this.errors = Arrays.asList(ex.getMessage());
	}

	public ApiErros(LivroNaoExisteException ex) {
		this.errors = Arrays.asList(ex.getMessage());
	}

	public ApiErros(ResponseStatusException ex) {
		this.errors = Arrays.asList(ex.getReason());
	}

}
