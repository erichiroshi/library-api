package com.example.library.auth;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.library.auth.dto.LoginRequestDTO;
import com.example.library.auth.dto.RefreshTokenDTO;
import com.example.library.auth.dto.TokenResponseDTO;
import com.example.library.jwt.JwtService;
import com.example.library.refresh_token.RefreshToken;
import com.example.library.refresh_token.RefreshTokenService;
import com.example.library.user.User;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

	private final AuthenticationManager authManager;
	private final JwtService jwtService;
	private final RefreshTokenService refreshTokenService;
	
	@PostMapping("/login")
	public TokenResponseDTO login(@RequestBody LoginRequestDTO request) {

		Authentication authentication = authManager
				.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));

		String accessToken = jwtService.generateToken((UserDetails) authentication.getPrincipal());
		
        RefreshToken refreshToken = refreshTokenService.create((User) authentication.getPrincipal());

		return new TokenResponseDTO(
						accessToken,
						refreshToken.getToken(),
						OffsetDateTime.ofInstant(jwtService.getExpirationDate(accessToken), ZoneId.systemDefault()));
	}
	
	@PostMapping("/refresh")
	public ResponseEntity<TokenResponseDTO> refresh(@RequestBody RefreshTokenDTO refreshToken) {

		RefreshToken refresh = refreshTokenService.validate(refreshToken.refreshToken());

		// usuario dono do refresh token
		User user = refresh.getUser();

        // gera novo access token
		String newAccess = jwtService.generateToken(user);

		// gera novo refresh token (rotate)
		RefreshToken newRefresh = refreshTokenService.create(user);

		return ResponseEntity
				.ok(new TokenResponseDTO(
						newAccess, 
						newRefresh.getToken(), 
						OffsetDateTime.ofInstant(jwtService.getExpirationDate(newAccess), ZoneId.systemDefault())));
	}

}
