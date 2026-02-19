package com.example.library.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record RefreshTokenDTO(

		@JsonProperty("refresh_token")
		@JsonAlias("refreshToken")
		String refreshToken

) {}
