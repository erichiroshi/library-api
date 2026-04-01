package com.example.catalogservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "library.events";
    public static final String BOOK_RESTORE_QUEUE = "catalog.book.restore";
    public static final String BOOK_RESTORE_ROUTING_KEY = "loan.book.restore";
    public static final String BOOK_RESTORE_DLQ = "catalog.book.restore.dlq";
    public static final String DLQ_EXCHANGE = "library.events.dlq";

    @Bean
    DirectExchange libraryExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    DirectExchange dlqExchange() {
        return new DirectExchange(DLQ_EXCHANGE, true, false);
    }

    @Bean
    Queue bookRestoreQueue() {
        return QueueBuilder.durable(BOOK_RESTORE_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", BOOK_RESTORE_QUEUE + ".dead")
                .build();
    }

    @Bean
    Queue bookRestoreDlq() {
        return QueueBuilder.durable(BOOK_RESTORE_DLQ).build();
    }

    @Bean
    Binding bookRestoreBinding(Queue bookRestoreQueue, DirectExchange libraryExchange) {
        return BindingBuilder
                .bind(bookRestoreQueue)
                .to(libraryExchange)
                .with(BOOK_RESTORE_ROUTING_KEY);
    }

    @Bean
    Binding bookRestoreDlqBinding(Queue bookRestoreDlq, DirectExchange dlqExchange) {
        return BindingBuilder
                .bind(bookRestoreDlq)
                .to(dlqExchange)
                .with(BOOK_RESTORE_QUEUE + ".dead");
    }

    @Bean
    MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                  MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}