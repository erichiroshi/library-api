package com.example.library.book;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.library.book.dto.BookCreateDTO;
import com.example.library.book.dto.BookResponseDTO;
import com.example.library.book.dto.PageResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Books", description = "Endpoints para gerenciamento de livros")
@RestController
@RequestMapping("/api/books")
public class BookController {

	private static final Logger log = LoggerFactory.getLogger(BookController.class);

	private final BookService bookService;

	public BookController(BookService bookService) {
		this.bookService = bookService;
	}

	@Operation(
		    summary = "Criar novo livro",
		    description = "Cria um novo livro com os dados fornecidos"
		)
	@ApiResponse(responseCode = "201", description = "Livro criado com sucesso")
	@ApiResponse(responseCode = "400", description = "Dados inválidos fornecidos")
	@PostMapping
	public ResponseEntity<BookResponseDTO> create(@Valid @RequestBody BookCreateDTO dto) {
		BookResponseDTO response = bookService.create(dto);
		URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();
		return ResponseEntity.created(uri).body(response);
	}

	@Operation(
		    summary = "Listar livros",
		    description = "Retorna uma lista paginada de livros existentes"
		)
	@ApiResponse(responseCode = "200", description = "Lista de livros retornada com sucesso")
	@GetMapping
	public ResponseEntity<PageResponseDTO<BookResponseDTO>> findAll(Pageable pageable) {
		return ResponseEntity.ok(bookService.findAll(pageable));
	}

	@Operation(
		    summary = "Buscar livro por ID",
		    description = "Retorna os dados de um livro existente"
		)
    @ApiResponse(responseCode = "200", description = "Livro encontrado")
	@ApiResponse(responseCode = "404", description = "Livro não encontrado")
	@GetMapping("/{id}")
	public ResponseEntity<BookResponseDTO> findById(@PathVariable Long id) {
		log.info("Requisição GET /books/{}", id);
		return ResponseEntity.ok(bookService.findById(id));
	}
	
	@Operation(
		    summary = "Deletar livro por ID",
		    description = "Remove um livro existente do sistema"
		)
	@ApiResponse(responseCode = "204", description = "Livro deletado com sucesso")
	@ApiResponse(responseCode = "404", description = "Livro não encontrado")
	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> deleteById(@PathVariable Long id) {
		bookService.deleteById(id);
		return ResponseEntity.noContent().build();
	}

}
