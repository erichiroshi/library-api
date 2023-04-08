package br.com.erichiroshi.libraryapi1.service.impl;

import java.util.Optional;

import org.springframework.stereotype.Service;

import br.com.erichiroshi.libraryapi1.model.entity.Book;
import br.com.erichiroshi.libraryapi1.model.repository.BookRepository;
import br.com.erichiroshi.libraryapi1.service.BookService;
import br.com.erichiroshi.libraryapi1.service.exception.BusinessException;

@Service
public class BookServiceImpl implements BookService {

	private BookRepository repository;

	public BookServiceImpl(BookRepository repository) {
		this.repository = repository;
	}

	@Override
	public Book save(Book book) {
		if (repository.existsByIsbn(book.getIsbn()))
			throw new BusinessException("Isbn já cadastrado.");
		return repository.save(book);
	}

	@Override
	public Optional<Book> getById(Long id) {
		return repository.findById(id);
	}

	@Override
	public void delete(Book book) {
		repository.deleteById(book.getId());
	}

}
