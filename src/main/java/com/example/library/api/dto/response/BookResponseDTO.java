package com.example.library.api.dto.response;

import java.util.Set;

public record BookResponseDTO(
		Long id,
		String title,
		String isbn,
		Integer publicationYear, 
		Integer availableCopies,
		Set<Long> authorIds,
		Long categoryId
) {}