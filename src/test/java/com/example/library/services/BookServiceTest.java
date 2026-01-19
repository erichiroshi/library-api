package com.example.library.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.library.api.dto.request.BookRequestDTO;
import com.example.library.api.dto.response.BookResponseDTO;
import com.example.library.api.mapper.BookMapper;
import com.example.library.domain.entities.Book;
import com.example.library.domain.exceptions.InvalidOperationException;
import com.example.library.domain.exceptions.ResourceNotFoundException;
import com.example.library.domain.repositories.AuthorRepository;
import com.example.library.domain.repositories.BookRepository;
import com.example.library.domain.repositories.CategoryRepository;
import com.example.library.domain.services.BookService;

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

	@InjectMocks
	private BookService bookService;

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

}
