package com.example.coursehub.auth.token;

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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EmailVerificationTokenRepositoryTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

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
        user.setStatus(UserStatus.PENDING);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        anotherUser = new User();
        anotherUser.setEmail("another@example.com");
        anotherUser.setPasswordHash("hashed");
        anotherUser.setFullName("Another User");
        anotherUser.setRole(Role.STUDENT);
        anotherUser.setStatus(UserStatus.PENDING);
        anotherUser.setCreatedAt(LocalDateTime.now());
        userRepository.save(anotherUser);
    }

    private EmailVerificationToken createToken(User owner, String tokenValue, LocalDateTime expiresAt) {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(owner);
        token.setToken(tokenValue);
        token.setExpiresAt(expiresAt);
        token.setUsed(false);
        token.setCreatedAt(LocalDateTime.now());
        return emailVerificationTokenRepository.save(token);
    }

    // deleteByUserId
    @Test
    void deleteByUserId_shouldDeleteOnlyTokensOfThatUser() {
        createToken(user, "token-1", LocalDateTime.now().plusHours(24));
        createToken(user, "token-2", LocalDateTime.now().plusHours(24));
        createToken(anotherUser, "token-3", LocalDateTime.now().plusHours(24));

        emailVerificationTokenRepository.deleteByUserId(user.getId());

        assertThat(emailVerificationTokenRepository.findByToken("token-1")).isEmpty();
        assertThat(emailVerificationTokenRepository.findByToken("token-2")).isEmpty();
        assertThat(emailVerificationTokenRepository.findByToken("token-3")).isPresent();
    }

    @Test
    void deleteByUserId_shouldDoNothing_whenUserHasNoTokens() {
        emailVerificationTokenRepository.deleteByUserId(user.getId());
        assertThat(emailVerificationTokenRepository.findAll()).isEmpty();
    }

    // deleteByExpiresAtBefore
    @Test
    void deleteByExpiresAtBefore_shouldDeleteOnlyExpiredTokens() {
        createToken(user, "expired", LocalDateTime.now().minusHours(1));
        createToken(anotherUser, "active", LocalDateTime.now().plusHours(24));

        int deleted = emailVerificationTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());

        assertThat(deleted).isEqualTo(1);
        assertThat(emailVerificationTokenRepository.findByToken("expired")).isEmpty();
        assertThat(emailVerificationTokenRepository.findByToken("active")).isPresent();
    }

    @Test
    void deleteByExpiresAtBefore_shouldReturnZero_whenNoExpiredTokens() {
        createToken(user, "active", LocalDateTime.now().plusHours(24));

        int deleted = emailVerificationTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());

        assertThat(deleted).isZero();
    }
}
