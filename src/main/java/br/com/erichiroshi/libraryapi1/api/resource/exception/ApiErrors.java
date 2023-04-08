package br.com.erichiroshi.libraryapi1.api.resource.exception;

import java.util.ArrayList;
import java.util.List;

import org.springframework.validation.BindingResult;

import lombok.Getter;

@Getter
public class ApiErrors {

	List<String> errors = new ArrayList<>();

	public ApiErrors(BindingResult bindingResult) {
		bindingResult.getAllErrors().forEach(error -> this.errors.add(error.getDefaultMessage()));
	}

}
