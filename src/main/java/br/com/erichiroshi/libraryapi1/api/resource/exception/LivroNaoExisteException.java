package br.com.erichiroshi.libraryapi1.api.resource.exception;

public class LivroNaoExisteException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public LivroNaoExisteException(String message) {
		super(message);
	}
}
