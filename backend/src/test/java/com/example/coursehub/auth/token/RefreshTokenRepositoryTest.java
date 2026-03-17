package com.example.coursehub.auth.token;

import com.example.coursehub.auth.token.RefreshToken;
import com.example.coursehub.auth.token.RefreshTokenRepository;
import com.example.coursehub.user.Role;
import com.example.coursehub.user.User;
import com.example.coursehub.user.UserRepository;
import com.example.coursehub.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RefreshTokenRepositoryTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed");
        user.setFullName("Test User");
        user.setRole(Role.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        anotherUser = new User();
        anotherUser.setEmail("another@example.com");
        anotherUser.setPasswordHash("hashed");
        anotherUser.setFullName("Another User");
        anotherUser.setRole(Role.STUDENT);
        anotherUser.setStatus(UserStatus.ACTIVE);
        anotherUser.setCreatedAt(LocalDateTime.now());
        userRepository.save(anotherUser);
    }

    private RefreshToken createToken(User owner, String tokenValue, boolean revoked, LocalDateTime expiresAt) {
        RefreshToken token = new RefreshToken();
        token.setUser(owner);
        token.setToken(tokenValue);
        token.setRevoked(revoked);
        token.setExpiresAt(expiresAt);
        token.setCreatedAt(LocalDateTime.now());
        return refreshTokenRepository.save(token);
    }

    @Test
    void revokeAllByUserId_shouldRevokeOnlyTokensOfThatUser() {
        createToken(user, "token-1", false, LocalDateTime.now().plusDays(7));
        createToken(user, "token-2", false, LocalDateTime.now().plusDays(7));
        createToken(anotherUser, "token-3", false, LocalDateTime.now().plusDays(7));

        int updated = refreshTokenRepository.revokeAllByUserId(user.getId());

        List<RefreshToken> userTokens = refreshTokenRepository.findByUserId(user.getId());
        List<RefreshToken> anotherUserTokens = refreshTokenRepository.findByUserId(anotherUser.getId());

        assertThat(updated).isEqualTo(2);
        assertThat(userTokens).allMatch(RefreshToken::isRevoked);
        assertThat(anotherUserTokens).allMatch(t -> !t.isRevoked());
    }

    @Test
    void deleteByExpiresAtBefore_shouldDeleteOnlyExpiredTokens() {
        createToken(user, "expired", false, LocalDateTime.now().minusDays(1));
        createToken(user, "active", false, LocalDateTime.now().plusDays(7));

        int deleted = refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());

        assertThat(deleted).isEqualTo(1);
        assertThat(refreshTokenRepository.findByToken("expired")).isEmpty();
        assertThat(refreshTokenRepository.findByToken("active")).isPresent();
    }
}