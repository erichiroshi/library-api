package com.example.loanservice.client;

import java.util.Optional;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.loanservice.client.dto.BookDTO;
import com.example.loanservice.client.fallback.BookClientFallback;

@FeignClient(
    name = "catalog-service",
    fallback = BookClientFallback.class
)
public interface BookClient {

    @GetMapping("/api/v1/books/{id}")
    Optional<BookDTO> findById(@PathVariable Long id);

    @PatchMapping("/api/v1/books/{id}/decrement")
    int decrementCopies(@PathVariable Long id);

    @PatchMapping("/api/v1/books/{id}/restore/{quantity}")
    void restoreCopies(@PathVariable Long id, @PathVariable int quantity);
}