package com.example.loanservice.messaging;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.example.loanservice.config.RabbitMQConfig;
import com.example.loanservice.messaging.event.BookRestoreEvent;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanEventPublisher - Unit Tests")
class LoanEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private LoanEventPublisher publisher;

    @Test
    @DisplayName("Deve publicar BookRestoreEvent no exchange correto")
    void shouldPublishBookRestoreEvent() {
        BookRestoreEvent event = new BookRestoreEvent(1L, Map.of(1L, 1));

        publisher.publishBookRestore(event);

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.BOOK_RESTORE_ROUTING_KEY,
                event
        );
    }
}