package com.example.library.author.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthorRequestDTO(

		@NotBlank
		String name,

		String biography
) {}
