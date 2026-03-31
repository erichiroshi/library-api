package com.example.catalogservice.book;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.catalogservice.book.dto.BookCreateDTO;
import com.example.catalogservice.book.dto.BookResponseDTO;
import com.example.catalogservice.common.dto.PageResponseDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Books", description = "Endpoints para gerenciamento de livros")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/books")
public class BookController {

	private static final Logger log = LoggerFactory.getLogger(BookController.class);

	private final BookService bookService;
	private final BookMediaService bookMediaService;

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
	
	@Operation(
		    summary = "Upload de capa do livro",
		    description = "Faz upload de uma imagem de capa para um livro existente e salva a URL no S3"
		)
		@ApiResponse(responseCode = "200", description = "Capa enviada com sucesso")
		@ApiResponse(responseCode = "404", description = "Livro não encontrado")
	@PostMapping("/{id}/cover")
	public ResponseEntity<URI> uploadCover(@PathVariable Long id, @RequestPart	("file") MultipartFile file) {
		URI uri = bookMediaService.uploadCover(id, file);
		return ResponseEntity.ok(uri);
	}
	
	@PatchMapping("/{id}/decrement")
	public ResponseEntity<Integer> decrementCopies(@PathVariable Long id) {
	    return ResponseEntity.ok(bookService.decrementCopies(id));
	}

	@PatchMapping("/{id}/restore/{quantity}")
	public ResponseEntity<Void> restoreCopies(@PathVariable Long id, @PathVariable int quantity) {
	    bookService.restoreCopies(id, quantity);
	    return ResponseEntity.noContent().build();
	}

}
