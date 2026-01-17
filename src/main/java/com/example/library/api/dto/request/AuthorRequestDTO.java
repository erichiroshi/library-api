package com.example.library.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AuthorRequestDTO(

		@NotBlank
		String name,

		String biography
) {}
