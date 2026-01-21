package com.example.library.api.dto.response;

import java.io.Serializable;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados de um livro")
public record BookResponseDTO(
		
	    @Schema(description = "Identificador do livro", example = "1")
		Long id,
		
	    @Schema(description = "Título do livro", example = "Clean Code")
		String title,
		
	    @Schema(description = "ISBN do livro", example = "9780132350884")
		String isbn,
		Integer publicationYear, 
		Integer availableCopies,
		Set<Long> authorIds,
		Long categoryId
) implements Serializable 
{}