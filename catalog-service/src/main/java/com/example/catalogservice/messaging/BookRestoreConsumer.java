package com.example.catalogservice.messaging;

import java.util.List;
import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.catalogservice.book.Book;
import com.example.catalogservice.book.BookRepository;
import com.example.catalogservice.config.RabbitMQConfig;
import com.example.catalogservice.messaging.event.BookRestoreEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookRestoreConsumer {

    private final BookRepository bookRepository;

    @RabbitListener(queues = RabbitMQConfig.BOOK_RESTORE_QUEUE)
    @Transactional
    public void onBookRestore(BookRestoreEvent event) {
        log.info("Received BookRestoreEvent: loanId={} books={}",
                event.loanId(), event.bookQuantities());

        Map<Long, Integer> bookQuantities = event.bookQuantities();
        List<Book> books = bookRepository.findAllById(bookQuantities.keySet());

        books.forEach(book -> {
            Integer quantity = bookQuantities.get(book.getId());
            if (quantity != null) {
                book.setAvailableCopies(book.getAvailableCopies() + quantity);
                log.debug("Copies restored: bookId={} title={} qty={}",
                        book.getId(), book.getTitle(), quantity);
            }
        });

        bookRepository.saveAll(books);

        log.info("BookRestoreEvent processed: loanId={} booksRestored={}",
                event.loanId(), books.size());
    }
}