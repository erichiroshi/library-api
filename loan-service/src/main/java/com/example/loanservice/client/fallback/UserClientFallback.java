package com.example.loanservice.client.fallback;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.loanservice.client.UserClient;
import com.example.loanservice.client.dto.UserDTO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UserClientFallback implements UserClient {

    @Override
    public Optional<UserDTO> findById(Long id) {
        log.error("auth-service unavailable — findById fallback for userId={}", id);
        return Optional.empty();
    }

    @Override
    public Optional<UserDTO> findByEmail(String email) {
        log.error("auth-service unavailable — findByEmail fallback for email={}", email);
        return Optional.empty();
    }
}