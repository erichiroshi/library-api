package com.example.library.security.dto;

public record LoginRequest(
		String username,
		String password
) {}