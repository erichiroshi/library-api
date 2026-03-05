package com.example.library.user;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserLookupServiceImpl - Unit Tests")
class UserLookupServiceImplTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private UserLookupServiceImpl service;

    @Test
    @DisplayName("should return user when found by id")
    void shouldReturnUserWhenFoundById() {
        User user = new User();
        when(repository.findById(1L)).thenReturn(Optional.of(user));

        Optional<User> result = service.findById(1L);

        assertThat(result).isPresent().contains(user);
    }

    @Test
    @DisplayName("should return empty when user not found by id")
    void shouldReturnEmptyWhenUserNotFoundById() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        Optional<User> result = service.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return user when found by email")
    void shouldReturnUserWhenFoundByEmail() {
        User user = new User();
        when(repository.findByEmail("user@email.com")).thenReturn(Optional.of(user));

        Optional<User> result = service.findByEmail("user@email.com");

        assertThat(result).isPresent().contains(user);
    }

    @Test
    @DisplayName("should return empty when user not found by email")
    void shouldReturnEmptyWhenUserNotFoundByEmail() {
        when(repository.findByEmail("ghost@email.com")).thenReturn(Optional.empty());

        Optional<User> result = service.findByEmail("ghost@email.com");

        assertThat(result).isEmpty();
    }
}