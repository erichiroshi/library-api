package com.example.library.refresh_token;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.library.user.User;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByToken(String token);

	Optional<RefreshToken> findByUser(User user);

	/**
	 * Deleta todos os refresh tokens expirados antes da data especificada.
	 * 
	 * Usado pelo RefreshTokenCleanupJob para limpeza agendada.
	 * 
	 * @param now Instant atual - tokens com expiryDate < now são deletados
	 * @return Número de tokens deletados
	 */
	@Modifying
	@Query("""
			DELETE FROM RefreshToken rt 
			WHERE rt.expiryDate < :now
			""")
	int deleteByExpiryDateBefore(@Param("now") Instant now);
}
