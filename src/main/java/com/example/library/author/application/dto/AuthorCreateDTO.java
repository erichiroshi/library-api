package com.example.library.author.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthorCreateDTO(

		@NotBlank
		String name,

		String biography
) {}
