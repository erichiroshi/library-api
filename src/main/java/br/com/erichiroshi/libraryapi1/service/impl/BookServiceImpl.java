package br.com.erichiroshi.libraryapi1.service.impl;

import org.springframework.stereotype.Service;

import br.com.erichiroshi.libraryapi1.model.entity.Book;
import br.com.erichiroshi.libraryapi1.model.repository.BookRepository;
import br.com.erichiroshi.libraryapi1.service.BookService;

@Service
public class BookServiceImpl implements BookService {

	private BookRepository repository;

	public BookServiceImpl(BookRepository repository) {
		this.repository = repository;
	}

	@Override
	public Book save(Book book) {
		return repository.save(book);
	}

}
