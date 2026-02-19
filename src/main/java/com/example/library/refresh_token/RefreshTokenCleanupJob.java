package com.example.library.refresh_token;

import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Job agendado para limpar refresh tokens expirados do banco de dados.
 * 
 * Tokens expirados são deletados quando alguém tenta usá-los (via validate()),
 * mas tokens que nunca são usados ficam acumulando no banco.
 * 
 * Este job roda diariamente às 2h da manhã para fazer limpeza preventiva.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupJob {

    private final RefreshTokenRepository repository;

    /**
     * Remove todos os refresh tokens expirados do banco.
     * 
     * Cron: "0 0 2 * * *" = todos dia às 02:00 AM
     * Formato: segundo minuto hora dia mês dia-da-semana
     * 
     * Configurável via property:
     * @Scheduled(cron = "${refresh-token.cleanup.cron:0 0 2 * * *}")
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired refresh tokens...");
        
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