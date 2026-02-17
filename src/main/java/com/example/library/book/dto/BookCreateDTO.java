package com.example.library.book.dto;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record BookCreateDTO(
		Long id,
	    
		@NotBlank
		String title,
		
	    @Size(min = 10, max = 15)
		String isbn,
		
	    @NotNull
		Integer publicationYear, 
		Integer availableCopies,
		
		@NotNull
		@NotEmpty(message = "At least one author must be informed")
		Set<Long> authorIds,
		
		Long categoryId
) {}