package com.example.library.author;

import java.net.URI;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.library.author.dto.AuthorCreateDTO;
import com.example.library.author.dto.AuthorResponseDTO;
import com.example.library.author.dto.PageResponseDTO;

import jakarta.validation.Valid;

@Tag(name = "Authors", description = "Endpoints para gerenciamento de autores")
@RestController
@RequestMapping("/api/authors")
public class AuthorController {

	private final AuthorService service;

	public AuthorController(AuthorService service) {
		this.service = service;
	}

	@Operation(
			summary = "Criar um novo autor", 
			description = "Cria um novo autor com os dados fornecidos")
	@PostMapping
	public ResponseEntity<AuthorResponseDTO> create(@RequestBody @Valid AuthorCreateDTO dto) {

		AuthorResponseDTO response = service.create(dto);
		URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();

		return ResponseEntity.created(uri).body(response);
	}

	@Operation(
			summary = "Obter autor por ID", 
			description = "Retorna os detalhes de um autor específico usando seu ID")
	@GetMapping("/{id}")
	public ResponseEntity<AuthorResponseDTO> findById(@PathVariable Long id) {
		return ResponseEntity.ok(service.findById(id));
	}

	@Operation(
			summary = "Listar autores com paginação", 
			description = "Retorna uma lista paginada de autores, permitindo controle sobre o número de resultados por página e ordenação")
	@GetMapping
	public ResponseEntity<PageResponseDTO<AuthorResponseDTO>> findAll(Pageable pageable) {
		return ResponseEntity.ok(service.findAll(pageable));
	}

	@Operation(
			summary = "Deletar autor por ID", 
			description = "Remove um autor específico usando seu ID. Requer permissão de administrador")
	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> deleteById(@PathVariable Long id) {
		service.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
