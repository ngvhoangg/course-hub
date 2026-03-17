package com.example.coursehub.auth;

import com.example.coursehub.auth.dto.LoginRequest;
import com.example.coursehub.auth.dto.RegisterRequest;
import com.example.coursehub.auth.dto.ResendVerificationRequest;
import com.example.coursehub.auth.token.EmailVerificationTokenRepository;
import com.example.coursehub.auth.token.RefreshTokenRepository;
import com.example.coursehub.auth.token.TokenCleanupService;
import com.example.coursehub.common.kafka.producer.EventProducer;
import com.example.coursehub.common.ratelimit.RateLimitService;
import com.example.coursehub.user.Role;
import com.example.coursehub.user.User;
import com.example.coursehub.user.UserRepository;
import com.example.coursehub.user.UserStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.example.coursehub.common.ratelimit.RateLimitFilter;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {
    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public EventProducer eventProducer() {
            return Mockito.mock(EventProducer.class);
        }
    }

    @MockitoBean
    private TokenCleanupService tokenCleanupService;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setEmail("user@example.com");
        activeUser.setPasswordHash(passwordEncoder.encode("Password1!"));
        activeUser.setFullName("User");
        activeUser.setRole(Role.STUDENT);
        activeUser.setStatus(UserStatus.ACTIVE);
        activeUser.setCreatedAt(LocalDateTime.now());
        userRepository.save(activeUser);
    }

    // ==================== register ====================

    @Test
    void register_shouldCreateUserInDb() throws Exception {
        RegisterRequest request = new RegisterRequest(
            "newuser@example.com", "Password1!", "New User"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        assertThat(userRepository.findByEmail("newuser@example.com")).isPresent();
        assertThat(userRepository.findByEmail("newuser@example.com").get().getStatus())
            .isEqualTo(UserStatus.PENDING);
    }

    @Test
    void register_shouldCreateVerificationToken() throws Exception {
        RegisterRequest request = new RegisterRequest(
            "newuser@example.com", "Password1!", "New User"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        User user = userRepository.findByEmail("newuser@example.com").orElseThrow();
        assertThat(emailVerificationTokenRepository.findAll())
            .anyMatch(t -> t.getUser().getId().equals(user.getId()));
    }

    @Test
    void register_shouldReturn409_whenEmailAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest(
            "user@example.com", "Password1!", "User"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    void register_shouldReturn400_whenBodyMissing() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    // ==================== login ====================

    @Test
    void login_shouldReturnAccessTokenAndSetCookie() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "Password1!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    void login_shouldSaveRefreshTokenInDb() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "Password1!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        assertThat(refreshTokenRepository.findByUserId(activeUser.getId())).hasSize(1);
    }

    @Test
    void login_shouldReturn403_whenEmailNotVerified() throws Exception {
        User pendingUser = new User();
        pendingUser.setEmail("pending@example.com");
        pendingUser.setPasswordHash(passwordEncoder.encode("Password1!"));
        pendingUser.setFullName("Pending");
        pendingUser.setRole(Role.STUDENT);
        pendingUser.setStatus(UserStatus.PENDING);
        pendingUser.setCreatedAt(LocalDateTime.now());
        userRepository.save(pendingUser);

        LoginRequest request = new LoginRequest("pending@example.com", "Password1!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    void login_shouldReturn404_whenUserNotFound() throws Exception {
        LoginRequest request = new LoginRequest("unknown@example.com", "Password1!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    // ==================== refresh ====================

    @Test
    void refresh_shouldIssueNewAccessTokenAndSetNewCookie() throws Exception {
        // login to get real refresh token
        String setCookieHeader = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new LoginRequest("user@example.com", "Password1!")
                )))
            .andReturn()
            .getResponse()
            .getHeader("Set-Cookie");

        String refreshToken = setCookieHeader.split(";")[0].split("=")[1];

        mockMvc.perform(post("/api/auth/refresh")
                .cookie(new Cookie("refreshToken", refreshToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    void refresh_shouldRevokeOldToken() throws Exception {
        String setCookieHeader = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new LoginRequest("user@example.com", "Password1!")
                )))
            .andReturn()
            .getResponse()
            .getHeader("Set-Cookie");

        String refreshToken = setCookieHeader.split(";")[0].split("=")[1];

        mockMvc.perform(post("/api/auth/refresh")
            .cookie(new Cookie("refreshToken", refreshToken)));

        assertThat(refreshTokenRepository.findByToken(refreshToken).get().isRevoked()).isTrue();
    }

    @Test
    void refresh_shouldReturn404_whenCookieMissing() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
            .andExpect(status().isNotFound());
    }

    @Test
    void refresh_shouldReturn401_whenTokenReused() throws Exception {
        String setCookieHeader = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new LoginRequest("user@example.com", "Password1!")
                )))
            .andReturn()
            .getResponse()
            .getHeader("Set-Cookie");

        String refreshToken = setCookieHeader.split(";")[0].split("=")[1];

        // first refresh — rotates token
        mockMvc.perform(post("/api/auth/refresh")
            .cookie(new Cookie("refreshToken", refreshToken)));

        // second refresh with old token — reuse detected
        mockMvc.perform(post("/api/auth/refresh")
                .cookie(new Cookie("refreshToken", refreshToken)))
            .andExpect(status().isUnauthorized());
    }

    // ==================== logout ====================

    @Test
    void logout_shouldRevokeTokenAndClearCookie() throws Exception {
        String setCookieHeader = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new LoginRequest("user@example.com", "Password1!")
                )))
            .andReturn()
            .getResponse()
            .getHeader("Set-Cookie");

        String refreshToken = setCookieHeader.split(";")[0].split("=")[1];

        mockMvc.perform(post("/api/auth/logout")
                .cookie(new Cookie("refreshToken", refreshToken)))
            .andExpect(status().isNoContent())
            .andExpect(header().exists("Set-Cookie"));

        assertThat(refreshTokenRepository.findByToken(refreshToken).get().isRevoked()).isTrue();
    }

    @Test
    void logout_shouldReturn204AndClearCookie_whenCookieMissing() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
            .andExpect(status().isNoContent())
            .andExpect(header().exists("Set-Cookie"));
    }
}
