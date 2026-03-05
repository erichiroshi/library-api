package com.example.library.book;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookRepositoryAdapter implements BookAvailabilityPort {

    private final BookRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Book> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    @Transactional
    public int decrementCopies(Long id) {
        return repository.decrementCopies(id);
    }

    @Override
    @Transactional
    public void restoreCopies(Long id, int quantity) {
        repository.findById(id).ifPresent(book ->
            book.setAvailableCopies(book.getAvailableCopies() + quantity)
        );
    }
}