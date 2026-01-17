package com.example.library.domain.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.library.domain.entities.User;

public interface UserRepository extends JpaRepository<User, Long> {

}
