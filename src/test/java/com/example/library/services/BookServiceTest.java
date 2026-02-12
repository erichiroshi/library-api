package com.example.library.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.example.library.author.Author;
import com.example.library.author.AuthorRepository;
import com.example.library.book.Book;
import com.example.library.book.BookMapper;
import com.example.library.book.BookRepository;
import com.example.library.book.BookService;
import com.example.library.book.dto.BookRequestDTO;
import com.example.library.book.dto.BookResponseDTO;
import com.example.library.category.Category;
import com.example.library.category.CategoryRepository;
import com.example.library.exceptions.exceptionsDeletar.InvalidOperationException;
import com.example.library.exceptions.exceptionsDeletar.ResourceNotFoundException;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

	@Mock
	private BookRepository bookRepository;

	@Mock
	private BookMapper bookMapper;

	@Mock
	AuthorRepository authorRepository;

	@Mock
	CategoryRepository categoryRepository;

	private MeterRegistry meterRegistry;
	
	private BookService bookService;
	
	@BeforeEach
	void setUp() {
		meterRegistry = new SimpleMeterRegistry();
		bookService = new BookService(bookRepository, authorRepository, categoryRepository, bookMapper, meterRegistry);
	}

	@Test
	void findById_shouldReturnBook_whenExists() {

		Book book = new Book();
		book.setId(1L);
		book.setTitle("Clean Code");

		when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

		when(bookMapper.toDTO(book)).thenReturn(new BookResponseDTO(1L, "Clean Code", null, null, null, null, null));

		BookResponseDTO result = bookService.findById(1L);

		assertNotNull(result);
		assertEquals("Clean Code", result.title());
		verify(bookRepository).findById(1L);
	}

	@Test
	void findById_shouldThrowException_whenNotFound() {

		when(bookRepository.findById(1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> bookService.findById(1L));
	}

	@Test
	void findById_shouldCallRepositoryOnce() {

		when(bookRepository.findById(1L)).thenReturn(Optional.of(new Book()));

		bookService.findById(1L);

		verify(bookRepository, times(1)).findById(1L);
		verify(bookMapper, times(1)).toDTO(any(Book.class));
	}

	@Test
	void create_shouldFail_whenNoAuthors() {

		BookRequestDTO dto = BookRequestDTO.builder().title("Domain-Driven Design").isbn("9780134757599")
				.publicationYear(2003).availableCopies(5).authorIds(Set.of()) // No authors
				.categoryId(1L).build();

		assertThrows(InvalidOperationException.class, () -> bookService.create(dto));

		verifyNoInteractions(bookRepository);
		verifyNoInteractions(authorRepository);
		verifyNoInteractions(categoryRepository);
		verifyNoInteractions(bookMapper);
	}
	
	@Test
	void create_shouldIncrementCounter() {
		BookRequestDTO dto = new BookRequestDTO(null, "new title", "123456123456", 2026, null, Set.of(1L, 2L), 1L);
		Book book = new Book(1L, "new title", "123456123456", 2026, null, new Category(1L, "Fiction"), Set.of(new Author(1L, "Author 1", null)));

		when(bookMapper.toEntity(dto)).thenReturn(book);
		when(bookRepository.save(book)).thenReturn(book);
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(new Category(1L, "Fiction")));
		when(authorRepository.findAllById(dto.authorIds())).thenReturn(List.of(new Author(1L, "Author 1", null)));

		Counter counter = meterRegistry.find("library.books.created").counter();
		
		double before = counter == null ? 0 : counter.count();
		
		bookService.create(dto);

		double after = meterRegistry.find("library.books.created").counter().count();
		
		assertNotNull(counter);
		assertEquals(before + 1, after, 0.0001);
	}

}
