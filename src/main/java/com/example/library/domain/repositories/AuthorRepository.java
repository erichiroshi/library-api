package com.example.library.domain.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.library.domain.entities.Author;

public interface AuthorRepository extends JpaRepository<Author, Long> {
}
