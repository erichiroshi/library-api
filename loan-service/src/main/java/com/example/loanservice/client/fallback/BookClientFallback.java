package com.example.loanservice.client.fallback;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.loanservice.client.BookClient;
import com.example.loanservice.client.dto.BookDTO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class BookClientFallback implements BookClient {

    @Override
    public Optional<BookDTO> findById(Long id) {
        log.error("catalog-service unavailable — findById fallback for bookId={}", id);
        return Optional.empty();
    }

    @Override
    public int decrementCopies(Long id) {
        log.error("catalog-service unavailable — decrementCopies fallback for bookId={}", id);
        return 0;
    }

    @Override
    public void restoreCopies(Long id, int quantity) {
        log.error("catalog-service unavailable — restoreCopies fallback for bookId={} quantity={}", id, quantity);
    }

	@Override
	public Optional<BookDTO> findInternalBooksById(Long id) {
        log.error("catalog-service unavailable — findInternalBooksById fallback for bookId={}", id);
		return Optional.empty();
	}

	@Override
	public int decrementInternalCopies(Long id) {
        log.error("catalog-service unavailable — decrementInternalCopies fallback for bookId={}", id);
		return 0;
	}
}