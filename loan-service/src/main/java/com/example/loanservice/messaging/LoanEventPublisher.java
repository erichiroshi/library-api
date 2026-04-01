package com.example.loanservice.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.example.loanservice.config.RabbitMQConfig;
import com.example.loanservice.messaging.event.BookRestoreEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishBookRestore(BookRestoreEvent event) {
        log.info("Publishing BookRestoreEvent: loanId={} books={}",
                event.loanId(), event.bookQuantities());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.BOOK_RESTORE_ROUTING_KEY,
                event
        );

        log.debug("BookRestoreEvent published successfully: loanId={}", event.loanId());
    }
}