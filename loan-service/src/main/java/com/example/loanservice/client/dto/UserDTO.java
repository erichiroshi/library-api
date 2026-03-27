package com.example.loanservice.client.dto;

import java.util.Set;

public record UserDTO(
    Long id,
    String name,
    String email,
    Set<String> roles
) {}