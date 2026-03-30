package com.example.catalogservice.book;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.micrometer.core.instrument.Counter;

import com.example.catalogservice.author.AuthorRepository;
import com.example.catalogservice.book.dto.BookCreateDTO;
import com.example.catalogservice.book.exception.BookAlreadyExistsException;
import com.example.catalogservice.book.mapper.BookMapper;
import com.example.catalogservice.category.CategoryRepository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

	@Mock
	BookRepository repository;
	@Mock
	BookMapper mapper;
	@Mock
	AuthorRepository authorRepository; // ← direto, não LookupService
	@Mock
	CategoryRepository categoryRepository; // ← direto, não LookupService
	@Mock
	Counter bookCreatedCounter;

	@InjectMocks
	BookService service;

    @Test
    void create_whenIsbnAlreadyExists_shouldThrowConflictException() {
        var dto = new BookCreateDTO(null, "Clean Code", "9780132350884", 2008, 3, Set.of(1L), 1L);
        when(repository.existsByIsbn(dto.isbn())).thenReturn(true);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(BookAlreadyExistsException.class)
                .hasMessageContaining("Book Already Exists. ISBN: 9780132350884");

        verify(repository, never()).save(any());
    }
}