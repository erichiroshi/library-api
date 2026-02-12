package com.example.library.book.dto;

import java.io.Serializable;
import java.util.List;

public record PageResponseDTO<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) implements Serializable 
{}
