package br.com.erichiroshi.libraryapi1.service;

import java.util.Optional;

import br.com.erichiroshi.libraryapi1.model.entity.Book;

public interface BookService {

	Book save(Book book);

	Optional<Book> getById(Long id);

	void delete(Book book);

}
