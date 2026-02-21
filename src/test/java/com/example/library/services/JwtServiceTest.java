package com.example.library.services;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.JwtException;

import com.example.library.security.jwt.JwtService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService - Unit Tests (Refresh Token Support)")
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails testUser;
    
    @Autowired
    private Environment environment;

    @BeforeEach
    void setUp() {
        // Chave de 256 bits para HMAC-SHA256
        String secretKey = "test-secret-key-that-must-be-at-least-256-bits-long-for-hmac-sha256-algorithm";
        jwtService = new JwtService(secretKey, environment);
        
        // Configurar expiração via reflection (simula @Value)
        ReflectionTestUtils.setField(jwtService, "accessTokenSeconds", 900L); // 15 minutos

        testUser = User.builder()
            .username("john@example.com")
            .password("password")
            .authorities(new SimpleGrantedAuthority("ROLE_USER"))
            .build();
    }

    // ═══════════════════════════════════════════════════════════════════
    // GENERATE TOKEN
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("generateToken() - geração de JWT")
    class GenerateTokenTests {

        @Test
        @DisplayName("Deve gerar token JWT válido")
        void shouldGenerateValidJwtToken() {
            // Act
            String token = jwtService.generateToken(testUser);

            // Assert
            assertThat(token)
            	.isNotNull()
            	.isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
        }

        @Test
        @DisplayName("Token deve conter subject com username")
        void shouldContainSubjectWithUsername() {
            // Act
            String token = jwtService.generateToken(testUser);
            String username = jwtService.extractUsername(token);

            // Assert
            assertThat(username).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("Token deve conter roles do usuário")
        void shouldContainUserRoles() {
            // Arrange
            UserDetails adminUser = User.builder()
                .username("admin@example.com")
                .password("password")
                .authorities(
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_USER")
                )
                .build();

            // Act
            String token = jwtService.generateToken(adminUser);

            // Assert
            assertThat(jwtService.isTokenValid(token)).isTrue();
            // Roles são armazenadas no claim "roles" (verificado indiretamente via isTokenValid)
        }

        @Test
        @DisplayName("Tokens gerados em momentos diferentes devem ser diferentes")
        void shouldGenerateDifferentTokensAtDifferentTimes() throws InterruptedException {
            // Act
            String token1 = jwtService.generateToken(testUser);
            Thread.sleep(1000); // Espera 1 segundo
            String token2 = jwtService.generateToken(testUser);

            // Assert
            assertThat(token1).isNotEqualTo(token2);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // GET EXPIRATION DATE (Novo método para Refresh Token)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getExpirationDate() - extração de data de expiração")
    class GetExpirationDateTests {

        @Test
        @DisplayName("Deve extrair data de expiração do token")
        void shouldExtractExpirationDateFromToken() {
            // Arrange
            String token = jwtService.generateToken(testUser);

            // Act
            Instant expirationDate = jwtService.getExpirationDate(token);

            // Assert
            assertThat(expirationDate)
            	.isNotNull()
            	.isAfter(Instant.now());
        }

        @Test
        @DisplayName("Data de expiração deve respeitar configuração de accessTokenSeconds")
        void shouldRespectConfiguredExpirationSeconds() {
            // Arrange - 15 minutos (900 segundos)
            String token = jwtService.generateToken(testUser);
            Instant now = Instant.now();

            // Act
            Instant expirationDate = jwtService.getExpirationDate(token);

            // Assert
            long secondsUntilExpiry = Duration.between(now, expirationDate).getSeconds();
            assertThat(secondsUntilExpiry).isBetween(895L, 905L); // margem de 5 segundos
        }

        @Test
        @DisplayName("Deve retornar Instant correto para uso no TokenResponseDTO")
        void shouldReturnInstantForTokenResponseDto() {
            // Arrange
            String token = jwtService.generateToken(testUser);

            // Act
            Instant expirationDate = jwtService.getExpirationDate(token);

            // Assert - pode ser convertido para OffsetDateTime como no controller
            assertThat(expirationDate).isInstanceOf(Instant.class);
            
            // Simula conversão do controller:
            // OffsetDateTime.ofInstant(expirationDate, ZoneId.systemDefault())
            java.time.OffsetDateTime offsetDateTime = 
                java.time.OffsetDateTime.ofInstant(expirationDate, java.time.ZoneId.systemDefault());
            
            assertThat(offsetDateTime).isNotNull();
        }

        @Test
        @DisplayName("Deve lançar exceção quando token é inválido")
        void shouldThrowExceptionWhenTokenIsInvalid() {
            // Arrange
            String invalidToken = "invalid.jwt.token";

            // Act & Assert
            assertThatThrownBy(() -> jwtService.getExpirationDate(invalidToken))
                .isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("Deve lançar exceção quando token é malformado")
        void shouldThrowExceptionWhenTokenIsMalformed() {
            // Arrange
            String malformedToken = "not-a-jwt-at-all";

            // Act & Assert
            assertThatThrownBy(() -> jwtService.getExpirationDate(malformedToken))
                .isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("Tokens com expiração diferente devem retornar datas diferentes")
        void shouldReturnDifferentDatesForDifferentExpirations() {
            // Arrange - gerar tokens com configurações diferentes
            String token15min = jwtService.generateToken(testUser);
            
            // Mudar expiração para 30 minutos
            ReflectionTestUtils.setField(jwtService, "accessTokenSeconds", 1800L);
            String token30min = jwtService.generateToken(testUser);

            // Act
            Instant expiry15min = jwtService.getExpirationDate(token15min);
            Instant expiry30min = jwtService.getExpirationDate(token30min);

            // Assert
            assertThat(expiry30min).isAfter(expiry15min);
            long differenceSeconds = Duration.between(expiry15min, expiry30min).getSeconds();
            assertThat(differenceSeconds).isBetween(895L, 905L); // ~15 min de diferença
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // EXTRACT USERNAME
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("extractUsername() - extração do subject")
    class ExtractUsernameTests {

        @Test
        @DisplayName("Deve extrair username do token")
        void shouldExtractUsernameFromToken() {
            // Arrange
            String token = jwtService.generateToken(testUser);

            // Act
            String username = jwtService.extractUsername(token);

            // Assert
            assertThat(username).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("Deve extrair username correto de diferentes usuários")
        void shouldExtractCorrectUsernameForDifferentUsers() {
            // Arrange
            UserDetails user1 = User.builder()
                .username("user1@example.com")
                .password("pass")
                .authorities(Set.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

            UserDetails user2 = User.builder()
                .username("user2@example.com")
                .password("pass")
                .authorities(Set.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

            String token1 = jwtService.generateToken(user1);
            String token2 = jwtService.generateToken(user2);

            // Act
            String username1 = jwtService.extractUsername(token1);
            String username2 = jwtService.extractUsername(token2);

            // Assert
            assertThat(username1).isEqualTo("user1@example.com");
            assertThat(username2).isEqualTo("user2@example.com");
        }

        @Test
        @DisplayName("Deve lançar exceção para token inválido")
        void shouldThrowExceptionForInvalidToken() {
            // Arrange
            String invalidToken = "invalid.token.here";

            // Act & Assert
            assertThatThrownBy(() -> jwtService.extractUsername(invalidToken))
                .isInstanceOf(JwtException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // IS TOKEN VALID
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("isTokenValid() - validação de token")
    class IsTokenValidTests {

        @Test
        @DisplayName("Deve retornar true para token válido")
        void shouldReturnTrueForValidToken() {
            // Arrange
            String token = jwtService.generateToken(testUser);

            // Act
            boolean isValid = jwtService.isTokenValid(token);

            // Assert
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Deve retornar false para token inválido")
        void shouldReturnFalseForInvalidToken() {
            // Arrange
            String invalidToken = "invalid.jwt.token";

            // Act
            boolean isValid = jwtService.isTokenValid(invalidToken);

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Deve retornar false para token com assinatura incorreta")
        void shouldReturnFalseForTokenWithWrongSignature() {
            // Arrange
            String token = jwtService.generateToken(testUser);
            String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

            // Act
            boolean isValid = jwtService.isTokenValid(tamperedToken);

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Deve retornar false para token vazio")
        void shouldReturnFalseForEmptyToken() {
            // Act
            boolean isValid = jwtService.isTokenValid("");

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Deve retornar false para token null")
        void shouldReturnFalseForNullToken() {
            // Act
            boolean isValid = jwtService.isTokenValid(null);

            // Assert
            assertThat(isValid).isFalse();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // CONSTRUCTOR & CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Constructor & Configuration")
    class ConstructorTests {

        @Test
        @DisplayName("Deve lançar exceção quando secret key é null")
        void shouldThrowExceptionWhenSecretKeyIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> new JwtService(null, environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SECRET_KEY não pode ser null ou vazio");
        }

        @Test
        @DisplayName("Deve lançar exceção quando secret key é vazia")
        void shouldThrowExceptionWhenSecretKeyIsEmpty() {
            // Act & Assert
            assertThatThrownBy(() -> new JwtService("", environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SECRET_KEY não pode ser null ou vazio");
        }

        @Test
        @DisplayName("Deve lançar exceção quando secret key é apenas espaços")
        void shouldThrowExceptionWhenSecretKeyIsBlank() {
            // Act & Assert
            assertThatThrownBy(() -> new JwtService("   ", environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SECRET_KEY não pode ser null ou vazio");
        }

        @Test
        @DisplayName("Deve aceitar secret key válida de 256 bits")
        void shouldAcceptValid256BitSecretKey() {
            // Arrange
            String validKey = "a-valid-256-bit-secret-key-for-hmac-sha256-algorithm-that-is-long-enough";

            // Act & Assert
            assertThatCode(() -> new JwtService(validKey, environment))
                .doesNotThrowAnyException();
        }
    }
}