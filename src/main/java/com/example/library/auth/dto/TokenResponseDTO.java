package com.example.library.auth.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenResponseDTO(

		@JsonProperty("access_token")
		String accessToken,

		@JsonProperty("refresh_token")
		String refreshToken,

		@JsonProperty("expires_in")
		OffsetDateTime expiresIn

) {}