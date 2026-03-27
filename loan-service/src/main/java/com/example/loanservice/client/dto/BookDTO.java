package com.example.loanservice.client.dto;

public record BookDTO(
    Long id,
    String title,
    Integer availableCopies
) {}