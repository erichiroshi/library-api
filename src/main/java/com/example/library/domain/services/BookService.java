package com.example.library.domain.services;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.library.api.dto.request.BookRequestDTO;
import com.example.library.api.dto.response.BookResponseDTO;
import com.example.library.api.mapper.BookMapper;
import com.example.library.domain.entities.Author;
import com.example.library.domain.entities.Book;
import com.example.library.domain.entities.Category;
import com.example.library.domain.repositories.AuthorRepository;
import com.example.library.domain.repositories.BookRepository;
import com.example.library.domain.repositories.CategoryRepository;

@Service
public class BookService {

	private final BookRepository bookRepository;
	private final AuthorRepository authorRepository;
	private final CategoryRepository categoryRepository;
	private final BookMapper bookMapper;

	public BookService(BookRepository bookRepository, AuthorRepository authorRepository,
			CategoryRepository categoryRepository, BookMapper bookMapper) {

		this.bookRepository = bookRepository;
		this.authorRepository = authorRepository;
		this.categoryRepository = categoryRepository;
		this.bookMapper = bookMapper;
	}

	@Transactional
	public BookResponseDTO create(BookRequestDTO dto) {

		Book book = bookMapper.toEntity(dto);

		Set<Author> authors = authorRepository.findAllById(dto.authorIds())
				.stream()
				.collect(Collectors.toSet());

		Category category = categoryRepository.findById(dto.categoryId()).get();

		book.setAuthors(authors);
		book.setCategory(category);

		Book saved = bookRepository.save(book);
		return bookMapper.toDTO(saved);
	}

	@Transactional(readOnly = true)
	public List<BookResponseDTO> findAll() {
		return bookRepository.findAll()
				.stream()
				.map(bookMapper::toDTO)
				.toList();
	}

	@Transactional(readOnly = true)
	public BookResponseDTO findById(Long id) {
		Book book = bookRepository.findById(id).get();
		return bookMapper.toDTO(book);
	}

}
