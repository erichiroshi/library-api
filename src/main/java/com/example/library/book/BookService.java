package com.example.library.book;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.library.author.Author;
import com.example.library.author.AuthorRepository;
import com.example.library.book.dto.BookCreateDTO;
import com.example.library.book.dto.BookResponseDTO;
import com.example.library.book.dto.PageResponseDTO;
import com.example.library.book.exception.BookAlreadyExistsException;
import com.example.library.book.exception.BookNotFoundException;
import com.example.library.book.exception.InvalidOperationException;
import com.example.library.book.mapper.BookMapper;
import com.example.library.category.Category;
import com.example.library.category.CategoryRepository;
import com.example.library.category.exception.CategoryNotFoundException;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class BookService {
	
    private static final Logger log = LoggerFactory.getLogger(BookService.class);

	private final BookRepository repository;
	private final AuthorRepository authorRepository;
	private final CategoryRepository categoryRepository;
	private final BookMapper mapper;
	
    private final Counter bookCreatedCounter;

	public BookService(BookRepository repository, AuthorRepository authorRepository,
			CategoryRepository categoryRepository, BookMapper bookMapper, MeterRegistry registry) {

		this.repository = repository;
		this.authorRepository = authorRepository;
		this.categoryRepository = categoryRepository;
		this.mapper = bookMapper;
        this.bookCreatedCounter =
                Counter.builder("library.books.created")
                       .description("Quantidade de livros criados")
                       .register(registry);

	}

	@CacheEvict(value = "books", allEntries = true)
	@Transactional
	public BookResponseDTO create(BookCreateDTO dto) {

		if (dto.authorIds().isEmpty()) {
			throw new InvalidOperationException();
		}
		
		if(repository.existsByIsbn(dto.isbn())){
			throw new BookAlreadyExistsException(dto.isbn());
		}
		
		Book book = mapper.toEntity(dto);

		log.info("Creating book: {}", book.getTitle());

		Set<Author> authors = new HashSet<>(authorRepository.findAllById(dto.authorIds()));

		Category category = categoryRepository.findById(dto.categoryId())
				.orElseThrow(() -> new CategoryNotFoundException(dto.categoryId()));
		
		if (authors.size() != dto.authorIds().size()) {
		    throw new InvalidOperationException(dto.authorIds());
		}
		
		book.getAuthors().addAll(authors);
		book.setCategory(category);

		Book saved = repository.save(book);
		
        bookCreatedCounter.increment();
        
		return mapper.toDTO(saved);
	}

	@Cacheable(
		    value = "books",
		    key = "'page0:size:' + #pageable.pageSize + ':sort:' + #pageable.sort.toString()",
		    condition = "#pageable.pageNumber == 0"
		)
	@Transactional(readOnly = true)
	public PageResponseDTO<BookResponseDTO> findAll(Pageable pageable) {
		 Page<Book> page = repository.findAll(pageable);
		    
		    List<BookResponseDTO> content =
		            page.getContent()
		                .stream()
		                .map(mapper::toDTO)
		                .toList();


		    return new PageResponseDTO<>(
		            content,
		            page.getNumber(),
		            page.getSize(),
		            page.getTotalElements(),
		            page.getTotalPages()
		    );
	}

    @Cacheable(value = "bookById", key = "#id")
	@Transactional(readOnly = true)
	public BookResponseDTO findById(Long id) {
		log.info("Searching book with id={}", id);
		
		try {
		    Thread.sleep(2000); // 2 segundos
		} catch (InterruptedException _) {
		    Thread.currentThread().interrupt();
		}

		Book book = find(id);
		
		return mapper.toDTO(book);
	}

    @Caching(evict = {
    	    @CacheEvict(value = "books", allEntries = true),
    	    @CacheEvict(value = "bookById", key = "#id")
    	})
	@Transactional
	public void deleteById(Long id) {
		find(id);
		repository.deleteById(id);
	}

	private Book find(Long id) {
		return repository.findById(id).orElseThrow(() -> {
			log.warn("Book not found: {}", id);
			return new BookNotFoundException(id);
		});
	}
}
