package com.example.library.api.dto.request;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookRequestDTO(
		Long id,
	    
		@NotBlank
		String title,
		
	    @Size(min = 10, max = 13)
		String isbn,
		
	    @NotNull
		Integer publicationYear, 
		Integer availableCopies,
		
		@NotNull
		Set<Long> authorIds,
		Long categoryId
) {}