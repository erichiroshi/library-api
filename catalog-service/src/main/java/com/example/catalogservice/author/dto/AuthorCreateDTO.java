package com.example.catalogservice.author.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthorCreateDTO(

		@NotBlank
		String name,

		String biography
) {}
