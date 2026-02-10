package com.example.library.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequestDTO(

		@NotBlank 
		@Size(max = 100) 
		String name
) {}
