package com.example.library.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record LogoutRequest(

		@JsonAlias("refresh_token")
		String refreshToken

) {

}
