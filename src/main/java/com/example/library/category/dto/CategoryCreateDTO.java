package com.example.library.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryCreateDTO(

		@NotBlank 
		@Size(max = 25) 
		String name
) {}
