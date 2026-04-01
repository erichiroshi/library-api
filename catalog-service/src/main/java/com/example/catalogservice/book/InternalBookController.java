package com.example.catalogservice.book;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.catalogservice.book.dto.BookResponseDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/internal/books")
@RequiredArgsConstructor
public class InternalBookController {

	private final BookService bookService;

	@GetMapping("/{id}")
	ResponseEntity<BookResponseDTO> findBooksById(@PathVariable Long id) {
		log.info("Internal Book - Finding book by ID: {}", id);
		return ResponseEntity.ok(bookService.findById(id));
	}

	@PatchMapping("/{id}/decrement")
	public ResponseEntity<Integer> decrementCopies(@PathVariable Long id) {
		return ResponseEntity.ok(bookService.decrementCopies(id));
	}
}