package com.example.library.book;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import com.example.library.book.dto.BookCreateDTO;
import com.example.library.book.exception.BookAlreadyExistsException;
import com.example.library.book.mapper.BookMapper;

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
	MeterRegistry meterRegistry;
	@Mock
	Counter bookCreatedCounter;

	@InjectMocks
	BookService service;

    @Test
    void create_whenIsbnAlreadyExists_shouldThrowConflictException() {
        // arrange
        var dto = new BookCreateDTO(null, "Clean Code", "9780132350884", 2008, 3, Set.of(1L), 1L);
        when(repository.existsByIsbn(dto.isbn())).thenReturn(true);

        // act & assert
        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(BookAlreadyExistsException.class)
                .hasMessageContaining("Book Already Exists. ISBN: 9780132350884");

        verify(repository, never()).save(any());
    }
}