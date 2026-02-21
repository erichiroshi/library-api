package com.example.library.security.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import jakarta.annotation.PostConstruct;

@Component
public class JwtService {

	private final SecretKey key;
	
    private final Environment environment;
   
	@Value("${jwt.access-token-seconds}")
	private Long accessTokenSeconds;
	
	@Value("${jwt.secret-key}")
	private String secret;

	public JwtService(@Value("${jwt.secret-key}") String secret, Environment environment) {
		if (secret == null || secret.trim().isEmpty()) {
			throw new IllegalStateException(
					"SECRET_KEY não pode ser null ou vazio. Verifique application.properties ou variáveis de ambiente.");
		}
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.environment = environment;
	}

	@PostConstruct
	public void validate() {
		if (secret.contains("dev-insecure") && !isDevelopmentProfile()) {
			throw new IllegalStateException("Insecure JWT key in non-dev environment!");
		}
		
		String[] profiles = environment.getActiveProfiles();
		String activeProfiles = String.join(", ", profiles);
		if (activeProfiles.contains("prod")) {
			Assert.notNull(secret, "JWT_SECRET_KEY must be set in prod");
			Assert.isTrue(!secret.contains("your-secret-key"), "Default JWT key not allowed in prod");
		}
	}

	private boolean isDevelopmentProfile() {
		String[] profiles = environment.getActiveProfiles();
		String activeProfiles = String.join(", ", profiles);
		return activeProfiles.contains("dev");
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
