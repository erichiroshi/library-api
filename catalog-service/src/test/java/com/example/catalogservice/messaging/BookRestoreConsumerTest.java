package com.example.catalogservice.messaging;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.catalogservice.book.Book;
import com.example.catalogservice.book.BookRepository;
import com.example.catalogservice.messaging.event.BookRestoreEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookRestoreConsumer - Unit Tests")
class BookRestoreConsumerTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookRestoreConsumer consumer;

    @Test
    @DisplayName("Deve restaurar cópias dos livros ao receber evento")
    void shouldRestoreCopiesOnEvent() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Clean Code");
        book.setAvailableCopies(3);

        when(bookRepository.findAllById(Set.of(1L)))
                .thenReturn(List.of(book));

        BookRestoreEvent event = new BookRestoreEvent(10L, Map.of(1L, 2));

        consumer.onBookRestore(event);

        assertThat(book.getAvailableCopies()).isEqualTo(5);
        verify(bookRepository).saveAll(List.of(book));
    }

    @Test
    @DisplayName("Deve processar múltiplos livros no mesmo evento")
    void shouldRestoreMultipleBooksOnEvent() {
        Book book1 = new Book();
        book1.setId(1L);
        book1.setAvailableCopies(2);

        Book book2 = new Book();
        book2.setId(2L);
        book2.setAvailableCopies(0);

        when(bookRepository.findAllById(Set.of(1L, 2L)))
                .thenReturn(List.of(book1, book2));

        BookRestoreEvent event = new BookRestoreEvent(10L, Map.of(1L, 1, 2L, 3));

        consumer.onBookRestore(event);

        assertThat(book1.getAvailableCopies()).isEqualTo(3);
        assertThat(book2.getAvailableCopies()).isEqualTo(3);
    }

    @Test
    @DisplayName("Não deve falhar quando lista de livros está vazia")
    void shouldNotFailWhenBooksEmpty() {
        when(bookRepository.findAllById(Set.of(99L)))
                .thenReturn(List.of());

        BookRestoreEvent event = new BookRestoreEvent(10L, Map.of(99L, 1));

        consumer.onBookRestore(event);

        verify(bookRepository).saveAll(List.of());
    }
}