package com.example.catalogservice.book;

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

import io.micrometer.core.instrument.Counter;

import com.example.catalogservice.author.Author;
import com.example.catalogservice.author.AuthorRepository;
import com.example.catalogservice.book.dto.BookCreateDTO;
import com.example.catalogservice.book.dto.BookResponseDTO;
import com.example.catalogservice.book.exception.BookAlreadyExistsException;
import com.example.catalogservice.book.exception.BookNotFoundException;
import com.example.catalogservice.book.exception.InvalidOperationException;
import com.example.catalogservice.book.mapper.BookMapper;
import com.example.catalogservice.category.Category;
import com.example.catalogservice.category.CategoryRepository;
import com.example.catalogservice.category.exception.CategoryNotFoundException;
import com.example.catalogservice.common.dto.PageResponseDTO;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class BookService {
	
    private static final Logger log = LoggerFactory.getLogger(BookService.class);

	private final BookRepository repository;
	private final CategoryRepository categoryRepository;
	private final AuthorRepository authorRepository;
	private final BookMapper mapper;
	
	private final Counter bookCreatedCounter;
	
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
		    key = "'size:' + #pageable.pageSize + ':sort:' + #pageable.sort",
		    condition = "#pageable.pageNumber == 0"
		)
	@Transactional(readOnly = true)
	public PageResponseDTO<BookResponseDTO> findAll(Pageable pageable) {
		 Page<Book> page = repository.findAll(pageable);
		    
		    List<BookResponseDTO> content = page.getContent()
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
		return mapper.toDTO(find(id));
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
    
    @Caching(evict = {
    		@CacheEvict(value = "books", allEntries = true),
    		@CacheEvict(value = "bookById", key = "#bookId")
    })
	void updateCoverImageUrl(long bookId, String url) {
		Book book = find(bookId);
		book.setCoverImageUrl(url);
		repository.save(book);
	}

	Book find(Long id) {
		return repository.findById(id).orElseThrow(() -> {
			log.warn("Book not found: {}", id);
			return new BookNotFoundException(id);
		});
	}
}
