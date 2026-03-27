package com.example.loanservice.client;

import java.util.Optional;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.loanservice.client.dto.UserDTO;
import com.example.loanservice.client.fallback.UserClientFallback;

@FeignClient(
    name = "auth-service",
    fallback = UserClientFallback.class
)
public interface UserClient {

    @GetMapping("/internal/users/{id}")
    Optional<UserDTO> findById(@PathVariable Long id);

    @GetMapping("/internal/users/by-email")
    Optional<UserDTO> findByEmail(@RequestParam String email);
}