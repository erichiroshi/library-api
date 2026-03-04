package com.example.library.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class ShedLockConfig {

    /**
     * Usa o próprio PostgreSQL como backend de lock distribuído.
     * Não requer infraestrutura extra — apenas a tabela `shedlock` (V007).
     *
     * defaultLockAtMostFor = "10m": tempo máximo que o lock pode ficar ativo.
     * Garante liberação automática se a instância travar ou morrer sem liberar o lock.
     */
    @Bean
    LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime() // usa o clock do banco — evita problemas com clock skew entre instâncias
                        .build()
        );
    }
}