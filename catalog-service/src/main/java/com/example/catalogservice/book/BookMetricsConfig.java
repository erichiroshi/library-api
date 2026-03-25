package com.example.catalogservice.book;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Configuration
class BookMetricsConfig {

    @Bean
    Counter bookCreatedCounter(MeterRegistry registry) {
        return Counter.builder("library.books.created")
                .description("Quantidade de livros criados")
                .register(registry);
    }
}