package com.example.library.category;

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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.library.category.dto.CategoryCreateDTO;
import com.example.library.category.dto.CategoryResponseDTO;
import com.example.library.common.dto.PageResponseDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Categories", description = "Endpoints para gerenciamento de categorias de livros")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

	private final CategoryService service;

	@Operation(
        summary = "Criar nova categoria",
        description = "Cria uma nova categoria de livros. Requer permissão de administrador"
    )
    @ApiResponse(responseCode = "201", description = "Categoria criada com sucesso")
	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<CategoryResponseDTO> create(@RequestBody @Valid CategoryCreateDTO dto) {
		CategoryResponseDTO response = service.create(dto);
		URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();
		return ResponseEntity.created(uri).body(response);
	}

	@Operation(
        summary = "Buscar categoria por ID",
        description = "Retorna os detalhes de uma categoria específica usando seu ID"
    )
    @ApiResponse(responseCode = "200", description = "Categoria encontrada")
    @ApiResponse(responseCode = "404", description = "Categoria não encontrada")
    @GetMapping("/{id}")
	public ResponseEntity<CategoryResponseDTO> findById(@PathVariable Long id) {
		return ResponseEntity.ok(service.findById(id));
	}

    @Operation(
        summary = "Listar categorias com paginação",
        description = "Retorna uma lista paginada de categorias"
    )
    @ApiResponse(responseCode = "200", description = "Lista de categorias retornada com sucesso")
    @GetMapping
	public ResponseEntity<PageResponseDTO<CategoryResponseDTO>> findAll(Pageable pageable) {
		return ResponseEntity.ok(service.findAll(pageable));
	}
	
    @Operation(
        summary = "Deletar categoria por ID",
        description = "Remove uma categoria existente do sistema. Requer permissão de administrador"
    )
    @ApiResponse(responseCode = "204", description = "Categoria deletada com sucesso")
    @ApiResponse(responseCode = "404", description = "Categoria não encontrada")
    @DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> deleteById(@PathVariable Long id) {
		service.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
