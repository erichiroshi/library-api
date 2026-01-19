package com.example.library.api.controllers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.library.api.dto.request.BookRequestDTO;
import com.example.library.api.dto.response.BookResponseDTO;
import com.example.library.domain.services.BookService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Books", description = "Gerenciamento de livros")
@RestController
@RequestMapping("/api/books")
public class BookController {

	private static final Logger log = LoggerFactory.getLogger(BookController.class);

	private final BookService bookService;

	public BookController(BookService bookService) {
		this.bookService = bookService;
	}

	@PostMapping
	public ResponseEntity<BookResponseDTO> create(@Valid @RequestBody BookRequestDTO dto) {
		BookResponseDTO created = bookService.create(dto);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@GetMapping
	public ResponseEntity<List<BookResponseDTO>> findAll() {
		return ResponseEntity.ok(bookService.findAll());
	}

	@Operation(
		    summary = "Buscar livro por ID",
		    description = "Retorna os dados de um livro existente"
		)
		@ApiResponses({
		    @ApiResponse(responseCode = "200", description = "Livro encontrado"),
		    @ApiResponse(responseCode = "404", description = "Livro não encontrado")
		})
	@GetMapping("/{id}")
	public ResponseEntity<BookResponseDTO> findById(@PathVariable Long id) {
		log.info("Requisição GET /books/{}", id);
		return ResponseEntity.ok(bookService.findById(id));
	}

}
