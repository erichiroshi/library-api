package com.example.library.refresh_token;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.library.refresh_token.exception.ExpiredRefreshTokenException;
import com.example.library.refresh_token.exception.InvalidRefreshTokenException;
import com.example.library.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
	
    private final RefreshTokenRepository repository;

    @Value("${jwt.refresh-token-days}")
    private Long durationRefreshToken;

    public RefreshToken create(User user) {

        // opcional: remover refresh tokens antigos
        Optional<RefreshToken> refreshToken = repository.findByUser(user);
        if (refreshToken.isPresent()) {
            repository.delete(refreshToken.get());
        }

        RefreshToken refresh = RefreshToken.builder()
                .token(UUID.randomUUID().toString() + UUID.randomUUID())
                .expiryDate(Instant.now().plus(Duration.ofDays(durationRefreshToken)))
                .user(user)
                .build();

        return repository.save(refresh);
    }

	public RefreshToken validate(String token) {
		RefreshToken refresh = repository.findByToken(token)
				.orElseThrow(() -> new InvalidRefreshTokenException(token));

		if (refresh.getExpiryDate().isBefore(Instant.now())) {
			repository.delete(refresh);
			throw new ExpiredRefreshTokenException(refresh.getExpiryDate());
		}

		return refresh;
	}
}