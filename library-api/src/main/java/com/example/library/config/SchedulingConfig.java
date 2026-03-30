package com.example.library.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita agendamento de tarefas com @Scheduled.
 * 
 * Necessário para que o RefreshTokenCleanupJob execute automaticamente.
 * 
 * Para DESABILITAR em testes, use:
 *   @TestPropertySource(properties = "spring.task.scheduling.enabled=false")
 * 
 * Ou crie um profile específico:
 *   @Profile("!test")
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Configuração adicional de thread pool (opcional)
    // @Bean
    // public TaskScheduler taskScheduler() {
    //     ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    //     scheduler.setPoolSize(5);
    //     scheduler.setThreadNamePrefix("scheduled-task-");
    //     scheduler.initialize();
    //     return scheduler;
    // }
}