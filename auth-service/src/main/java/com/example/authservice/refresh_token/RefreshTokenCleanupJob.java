package com.example.authservice.refresh_token;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

/**
 * Job agendado para limpar refresh tokens expirados do banco de dados.
 *
 * Separação intencional entre lock e transação:
 * - @SchedulerLock NÃO pode estar num método @Transactional
 *   porque o lock precisa ser adquirido e liberado FORA da transação.
 *   Se estivessem juntos, o lock só seria liberado no commit,
 *   anulando a proteção distribuída do ShedLock.
 * - A transação fica em RefreshTokenCleanupService, que é chamado
 *   após o lock ser adquirido.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupJob {

    private final RefreshTokenCleanupService cleanupService;

    @Scheduled(cron = "0 0 2 * * *")
    @SchedulerLock(
            name = "refreshTokenCleanupJob",
            lockAtLeastFor = "30m",
            lockAtMostFor = "1h"
    )
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired refresh tokens...");
        cleanupService.deleteExpiredTokens();
    }
}