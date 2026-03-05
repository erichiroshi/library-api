package com.example.library.book;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookLookupServiceImpl - Unit Tests")
class BookLookupServiceImplTest {

    @Mock
    private BookRepository repository;

    @InjectMocks
    private BookLookupServiceImpl service;

    @Test
    @DisplayName("should return book when found by id")
    void shouldReturnBookWhenFoundById() {
        Book book = new Book();
        when(repository.findById(1L)).thenReturn(Optional.of(book));

        Optional<Book> result = service.findById(1L);

        assertThat(result).isPresent().contains(book);
    }

    @Test
    @DisplayName("should return empty when book not found by id")
    void shouldReturnEmptyWhenBookNotFoundById() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        Optional<Book> result = service.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should delegate decrementCopies to repository")
    void shouldDelegateDecrementCopiesToRepository() {
        when(repository.decrementCopies(1L)).thenReturn(1);

        int updated = service.decrementCopies(1L);

        assertThat(updated).isEqualTo(1);
        verify(repository).decrementCopies(1L);
    }

    @Test
    @DisplayName("should return zero when no copies available to decrement")
    void shouldReturnZeroWhenNoCopiesAvailableToDecrement() {
        when(repository.decrementCopies(1L)).thenReturn(0);

        int updated = service.decrementCopies(1L);

        assertThat(updated).isZero();
    }

    @Test
    @DisplayName("should restore copies when book exists")
    void shouldRestoreCopiesWhenBookExists() {
        Book book = new Book();
        book.setAvailableCopies(2);
        when(repository.findById(1L)).thenReturn(Optional.of(book));

        service.restoreCopies(1L, 3);

        assertThat(book.getAvailableCopies()).isEqualTo(5);
    }

    @Test
    @DisplayName("should do nothing on restoreCopies when book not found")
    void shouldDoNothingOnRestoreCopiesWhenBookNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        // não lança exceção — comportamento esperado é silencioso
        service.restoreCopies(99L, 3);

        verify(repository).findById(99L);
    }
}