package com.example.library.book;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.library.loan.event.LoanCanceledEvent;
import com.example.library.loan.event.LoanReturnedEvent;

import lombok.RequiredArgsConstructor;

/**
 * Listener responsável por manter o estoque de cópias dos livros
 * em resposta a eventos do domínio Loan.
 *
 * Separação de responsabilidade:
 * - LoanService publica o evento e não sabe nada sobre Book
 * - BookEventListener reage ao evento e atualiza as cópias
 *
 * @EventListener é síncrono e transacional por padrão neste contexto:
 * o listener roda na mesma transação do publicador quando anotado
 * com @Transactional — garantindo atomicidade entre criar o empréstimo
 * e decrementar as cópias enquanto ainda somos monolito.
 *
 * Quando migrarmos para microservices, este listener será substituído
 * por um consumer Kafka/RabbitMQ sem mudança nos eventos.
 */
@Component
@RequiredArgsConstructor
public class BookEventListener {

    private static final Logger log = LoggerFactory.getLogger(BookEventListener.class);

    private final BookRepository bookRepository;

    /**
     * Restaura as cópias disponíveis quando um empréstimo é devolvido.
     */
    @EventListener
    @Transactional
    public void onLoanReturned(LoanReturnedEvent event) {
        log.info("Restoring copies on loan return: loanId={}", event.loanId());

        List<Book> books = bookRepository.findAllById(event.bookQuantities().keySet());

        books.forEach(book -> {
            Integer qty = event.bookQuantities().get(book.getId());
            if (qty != null) {
                book.setAvailableCopies(book.getAvailableCopies() + qty);
                log.debug("Copies restored: bookId={} title={} qty={}", book.getId(), book.getTitle(), qty);
            }
        });
    }

    /**
     * Restaura as cópias disponíveis quando um empréstimo é cancelado.
     */
    @EventListener
    @Transactional
    public void onLoanCanceled(LoanCanceledEvent event) {
        log.info("Restoring copies on loan cancel: loanId={}", event.loanId());

        List<Book> books = bookRepository.findAllById(event.bookQuantities().keySet());

        books.forEach(book -> {
            Integer qty = event.bookQuantities().get(book.getId());
            if (qty != null) {
                book.setAvailableCopies(book.getAvailableCopies() + qty);
                log.debug("Copies restored: bookId={} title={} qty={}", book.getId(), book.getTitle(), qty);
            }
        });
    }
}