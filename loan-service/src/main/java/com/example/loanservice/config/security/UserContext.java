package com.example.loanservice.config.security;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Component
@RequestScope
@RequiredArgsConstructor
public class UserContext {

	private final HttpServletRequest request;

	public String getUserId() {
		return request.getHeader("X-User-Id");
	}

	public boolean hasRole(String role) {
		String roles = request.getHeader("X-User-Roles");
		if (roles == null)
			return false;
		
		return List.of(roles.split(",")).contains(role);
	}

	public boolean isAdmin() {
		return hasRole("ROLE_ADMIN");
	}
}
