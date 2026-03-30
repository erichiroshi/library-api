package com.example.library.refresh_token;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Serviço responsável pela limpeza transacional de refresh tokens expirados.
 *
 * Separado do RefreshTokenCleanupJob intencionalmente:
 * - O Job gerencia o lock distribuído (ShedLock) sem transação
 * - Este Service gerencia a transação sem lock
 *
 * Esta separação garante que o ShedLock adquira e libere o lock
 * FORA da transação — comportamento correto e esperado pelo ShedLock.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupService {

    private final RefreshTokenRepository repository;

    @Transactional
    public void deleteExpiredTokens() {
        try {
            int deletedCount = repository.deleteByExpiryDateBefore(Instant.now());

            if (deletedCount > 0) {
                log.info("Deleted {} expired refresh tokens", deletedCount);
            } else {
                log.debug("No expired refresh tokens found");
            }
        } catch (Exception e) {
            log.error("Error cleaning up expired refresh tokens", e);
        }
    }
}