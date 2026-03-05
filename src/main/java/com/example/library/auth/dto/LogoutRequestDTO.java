package com.example.library.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record LogoutRequestDTO(
		@JsonAlias("refresh_token") 
		String refreshToken
) {}