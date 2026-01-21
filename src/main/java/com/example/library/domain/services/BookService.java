package com.example.library.domain.services;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.library.api.dto.request.BookRequestDTO;
import com.example.library.api.dto.response.BookResponseDTO;
import com.example.library.api.mapper.BookMapper;
import com.example.library.domain.entities.Author;
import com.example.library.domain.entities.Book;
import com.example.library.domain.entities.Category;
import com.example.library.domain.exceptions.BusinessException;
import com.example.library.domain.exceptions.InvalidOperationException;
import com.example.library.domain.exceptions.ResourceNotFoundException;
import com.example.library.domain.repositories.AuthorRepository;
import com.example.library.domain.repositories.BookRepository;
import com.example.library.domain.repositories.CategoryRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class BookService {
	
    private static final Logger log = LoggerFactory.getLogger(BookService.class);

	private final BookRepository bookRepository;
	private final AuthorRepository authorRepository;
	private final CategoryRepository categoryRepository;
	private final BookMapper bookMapper;
	
    private final Counter bookCreatedCounter;

	public BookService(BookRepository bookRepository, AuthorRepository authorRepository,
			CategoryRepository categoryRepository, BookMapper bookMapper, MeterRegistry registry) {

		this.bookRepository = bookRepository;
		this.authorRepository = authorRepository;
		this.categoryRepository = categoryRepository;
		this.bookMapper = bookMapper;
        this.bookCreatedCounter =
                Counter.builder("library.books.created")
                       .description("Quantidade de livros criados")
                       .register(registry);

	}

	@Transactional
	public BookResponseDTO create(BookRequestDTO dto) {

		if (dto.authorIds().isEmpty()) {
			throw new InvalidOperationException("Livro deve possuir ao menos um autor");
		}
		
		if(bookRepository.existsByIsbn(dto.isbn())){
			throw new BusinessException("ISBN já existe");
		}
		
		Book book = bookMapper.toEntity(dto);

		log.info("Creating book: {}", book.getTitle());

		Set<Author> authors = authorRepository.findAllById(dto.authorIds())
				.stream()
				.collect(Collectors.toSet());

		Category category = categoryRepository.findById(dto.categoryId())
				.orElseThrow(() -> new ResourceNotFoundException("Category not found: " + dto.categoryId()));
		
		if (authors.isEmpty()) {
	        throw new InvalidOperationException("Invalid authors");
	    }
		
		book.setAuthors(authors);
		book.setCategory(category);

		Book saved = bookRepository.save(book);
		
        bookCreatedCounter.increment();
        
		return bookMapper.toDTO(saved);
	}

	@Cacheable(value = "books-list")
	@Transactional(readOnly = true)
	public List<BookResponseDTO> findAll() {
		return bookRepository.findAll()
				.stream()
				.map(bookMapper::toDTO)
				.toList();
	}

    @Cacheable(value = "books-by-id", key = "#id")
	@Transactional(readOnly = true)
	public BookResponseDTO findById(Long id) {
		log.info("Searching book with id={}", id);

		Book book = bookRepository.findById(id).orElseThrow(() -> {
			log.warn("Book not found: {}", id);
			return new ResourceNotFoundException("Book not found. Id: " + id);
		});
		
		return bookMapper.toDTO(book);
	}

}
