package com.example.library.security.web;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.library.security.dto.LoginRequest;
import com.example.library.security.dto.LoginResponse;
import com.example.library.security.service.JwtService;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final AuthenticationManager authManager;
	private final JwtService jwtService;
	

	public AuthController(AuthenticationManager authManager, JwtService jwtService) {
		this.authManager = authManager;
		this.jwtService = jwtService;
	}

	@PostMapping("/login")
	public LoginResponse login(@RequestBody LoginRequest request) {

		Authentication authentication = authManager
				.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));

		String token = jwtService.generateToken((UserDetails) authentication.getPrincipal());

		return new LoginResponse(token);
	}
}
