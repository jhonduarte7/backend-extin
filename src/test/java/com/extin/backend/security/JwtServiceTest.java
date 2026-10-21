package com.extin.backend.security;

import com.extin.backend.models.entities.User;
import com.extin.backend.models.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests verifying JWT token generation, claims extraction, and signature validation in {@link JwtService}.
 */
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3600000L); // 1 hour
    }

    @Test
    @DisplayName("Should generate a valid token and extract username and claims")
    void testGenerateAndValidateToken() {
        User user = new User("admin", "hashedpassword", "admin@extin.com", Role.ADMIN);

        String token = jwtService.generateToken(user);
        assertThat(token).isNotBlank();

        String extractedUsername = jwtService.extractUsername(token);
        assertThat(extractedUsername).isEqualTo("admin");

        boolean isValid = jwtService.isTokenValid(token, user);
        assertThat(isValid).isTrue();

        String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
        assertThat(role).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Should invalidate token for mismatched user")
    void testTokenInvalidForDifferentUser() {
        User user1 = new User("admin", "hashedpassword", Role.ADMIN);
        User user2 = new User("otheruser", "hashedpassword", Role.USER);

        String token = jwtService.generateToken(user1);
        boolean isValid = jwtService.isTokenValid(token, user2);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should report expired when token expiration is zero or negative")
    void testExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L); // Expired immediately
        User user = new User("admin", "hashedpassword", Role.ADMIN);

        String token = jwtService.generateToken(user);
        boolean isValid = jwtService.isTokenValid(token, user);

        assertThat(isValid).isFalse();
        assertThat(jwtService.isTokenExpired(token)).isTrue();
    }
}
