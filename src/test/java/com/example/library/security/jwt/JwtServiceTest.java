package com.example.library.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService - Unit Tests")
class JwtServiceTest {

	@Mock
	private Environment env;
	
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
            "test-secret-key-for-unit-tests-only-256bits-long!!", env);
        try {
            var field = JwtService.class.getDeclaredField("accessTokenSeconds");
            field.setAccessible(true);
            field.set(jwtService, 900L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("should extract username from token")
    void shouldExtractUsernameFromToken() {
        UserDetails user = mock(UserDetails.class);
        doReturn("test@email.com").when(user).getUsername();
        doReturn(List.of()).when(user).getAuthorities();

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("test@email.com");
    }

    @Test
    @DisplayName("should extract roles from token")
    void shouldExtractRolesFromToken() {
        UserDetails user = mock(UserDetails.class);
        doReturn("test@email.com").when(user).getUsername();
        doReturn(List.of(
            new SimpleGrantedAuthority("ROLE_USER"),
            new SimpleGrantedAuthority("ROLE_ADMIN")
        )).when(user).getAuthorities();

        String token = jwtService.generateToken(user);
        List<String> roles = jwtService.extractRoles(token);

        assertThat(roles).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("should return empty roles when none present in token")
    void shouldReturnEmptyRolesWhenNonePresent() {
        UserDetails user = mock(UserDetails.class);
        doReturn("test@email.com").when(user).getUsername();
        doReturn(List.of()).when(user).getAuthorities();

        String token = jwtService.generateToken(user);
        List<String> roles = jwtService.extractRoles(token);

        assertThat(roles).isEmpty();
    }

    @Test
    @DisplayName("should validate a valid token")
    void shouldValidateToken() {
        UserDetails user = mock(UserDetails.class);
        doReturn("test@email.com").when(user).getUsername();
        doReturn(List.of()).when(user).getAuthorities();

        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.isTokenValid("invalid.token.here")).isFalse();
    }
}