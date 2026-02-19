package com.example.library.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtService {

	   private final SecretKey key;
	   
	   @Value("${jwt.access-token-seconds}")
	   private Long accessTokenSeconds;
		
	    public JwtService(@Value("${jwt.secret-key}") String secret) {
	        if (secret == null || secret.trim().isEmpty()) {
	            throw new IllegalStateException("SECRET_KEY não pode ser null ou vazio. Verifique application.properties ou variáveis de ambiente.");
	        }
	        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	    }

    public String generateToken(UserDetails user) {
		return Jwts.builder()
                .subject(user.getUsername())
                .claim("roles", user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(accessTokenSeconds, ChronoUnit.SECONDS)))
                .signWith(key)
                .compact();
    }
    
    public Instant getExpirationDate(String token) {
		return parseClaims(token).getExpiration().toInstant();
	}

	public String extractUsername(String token) {
		return parseClaims(token).getSubject();
	}

	public boolean isTokenValid(String token) {
		try {
			parseClaims(token);
			return true;
		} catch (Exception _) {
			return false;
		}
	}

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
