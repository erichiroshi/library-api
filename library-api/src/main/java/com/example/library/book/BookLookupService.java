package com.example.library.book;

import java.util.Optional;

public interface BookLookupService {
	Optional<Book> findById(Long id);

	int decrementCopies(Long id);

	void restoreCopies(Long id, int quantity);
}