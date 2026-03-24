package com.example.gateway.security;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtService {

    @Value("${jwt.secret-key}")
    private String secret;

    private SecretKey key;

	public JwtService(@Value("${jwt.secret-key}") String secret) {
		if (secret == null || secret.trim().isEmpty()) {
			throw new IllegalStateException(
					"SECRET_KEY não pode ser null ou vazio. Verifique application.properties ou variáveis de ambiente.");
		}
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}
    
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception _) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public List<String> extractRoles(String token) {
        Claims claims = parseClaims(token);
        Object roles = claims.get("roles");
        if (roles instanceof List<?> list) {
            return list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        return List.of();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}