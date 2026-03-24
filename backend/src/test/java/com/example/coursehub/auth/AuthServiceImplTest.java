package com.example.coursehub.auth;

import com.example.coursehub.auth.dto.AuthResult;
import com.example.coursehub.auth.jwt.JwtProperties;
import com.example.coursehub.auth.jwt.JwtTokenProvider;
import com.example.coursehub.auth.token.*;
import com.example.coursehub.common.exception.ErrorCode;
import com.example.coursehub.common.exception.UserError;
import com.example.coursehub.common.kafka.event.EmailVerificationEvent;
import com.example.coursehub.common.kafka.producer.EventProducer;
import com.example.coursehub.user.Role;
import com.example.coursehub.user.User;
import com.example.coursehub.user.UserRepository;
import com.example.coursehub.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtProperties jwtProperties;
    @Mock private EventProducer eventProducer;
    @Mock private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User activeUser;
    private User pendingUser;
    private User disabledUser;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setId(1L);
        activeUser.setEmail("active@example.com");
        activeUser.setPasswordHash("hashed");
        activeUser.setRole(Role.STUDENT);
        activeUser.setStatus(UserStatus.ACTIVE);

        pendingUser = new User();
        pendingUser.setId(2L);
        pendingUser.setEmail("pending@example.com");
        pendingUser.setStatus(UserStatus.PENDING);

        disabledUser = new User();
        disabledUser.setId(3L);
        disabledUser.setEmail("disabled@example.com");
        disabledUser.setStatus(UserStatus.DISABLED);
    }

    // ==================== register ====================

    @Test
    void register_shouldSaveUserAndPublishEvent() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("hashed");

        authService.register("new@example.com", "password", "New User");

        verify(userRepository).save(any(User.class));
        verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
        verify(eventProducer).sendEmailVerification(any(EmailVerificationEvent.class));
    }

    @Test
    void register_shouldPublishEventWithCorrectEmail() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");

        authService.register("new@example.com", "password", "New User");

        ArgumentCaptor<EmailVerificationEvent> captor = ArgumentCaptor.forClass(EmailVerificationEvent.class);
        verify(eventProducer).sendEmailVerification(captor.capture());
        assertThat(captor.getValue().toEmail()).isEqualTo("new@example.com");
    }

    @Test
    void register_shouldThrow_whenEmailAlreadyExists() {
        when(userRepository.existsByEmail("active@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("active@example.com", "password", "User"))
            .isInstanceOf(UserError.class)
            .hasMessageContaining(ErrorCode.EMAIL_ALREADY_EXISTS.getMessage());
    }

    // ==================== login ====================

    @Test
    void login_shouldReturnAuthResult_whenCredentialsValid() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
            "active@example.com", null,
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
        when(userRepository.findByEmail("active@example.com")).thenReturn(Optional.of(activeUser));
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenProvider.generateAccessToken(auth)).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(auth)).thenReturn("refresh-token");
        when(jwtProperties.getRefreshTokenExpiration()).thenReturn(604800000L);

        AuthResult result = authService.login("active@example.com", "password");

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_shouldThrow_whenUserNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("unknown@example.com", "password"))
            .isInstanceOf(UserError.class)
            .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void login_shouldThrow_whenEmailNotVerified() {
        when(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(pendingUser));

        assertThatThrownBy(() -> authService.login("pending@example.com", "password"))
            .isInstanceOf(UserError.class)
            .hasMessageContaining(ErrorCode.EMAIL_NOT_VERIFIED.getMessage());
    }

    @Test
    void login_shouldThrow_whenAccountDisabled() {
        when(userRepository.findByEmail("disabled@example.com")).thenReturn(Optional.of(disabledUser));

        assertThatThrownBy(() -> authService.login("disabled@example.com", "password"))
            .isInstanceOf(UserError.class)
            .hasMessageContaining(ErrorCode.ACCOUNT_DISABLED.getMessage());
    }

    @Test
    void login_shouldThrow_whenPasswordInvalid() {
        when(userRepository.findByEmail("active@example.com")).thenReturn(Optional.of(activeUser));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login("active@example.com", "wrongpassword"))
            .isInstanceOf(BadCredentialsException.class);
    }

    // ==================== verifyEmail ====================

    @Test
    void verifyEmail_shouldActivateUser_whenTokenValid() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(activeUser);
        token.setUsed(false);
        token.setExpiresAt(LocalDateTime.now().plusHours(1));

        when(emailVerificationTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        authService.verifyEmail("valid-token");

        assertThat(activeUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(token.isUsed()).isTrue();
    }

    @Test
    void verifyEmail_shouldThrow_whenTokenNotFound() {
        when(emailVerificationTokenRepository.findByToken("invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail("invalid"))
            .isInstanceOf(UserError.class)
            .hasMessageContaining(ErrorCode.VERIFICATION_TOKEN_NOT_FOUND.getMessage());
    }

    @Test
    void verifyEmail_shouldThrow_whenTokenAlreadyUsed() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUsed(true);
        token.setExpiresAt(LocalDateTime.now().plusHours(1));

        when(emailVerificationTokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.verifyEmail("used-token"))
            .isInstanceOf(UserError.class)
            .hasMessageContaining(ErrorCode.VERIFICATION_TOKEN_ALREADY_USED.getMessage());
    }

    @Test
    void verifyEmail_shouldThrow_whenTokenExpired() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUsed(false);
        token.setExpiresAt(LocalDateTime.now().minusHours(1));

        when(emailVerificationTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.verifyEmail("expired-token"))
            .isInstanceOf(UserError.class)
            .hasMessageContaining(ErrorCode.VERIFICATION_TOKEN_EXPIRED.getMessage());
    }

    // ==================== resendVerification ====================

    @Test
    void resendVerification_shouldDeleteOldTokensAndPublishEvent() {
        when(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(pendingUser));

        authService.resendVerification("pending@example.com");

        verify(emailVerificationTokenRepository).deleteByUserId(pendingUser.getId());
        verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
        verify(eventProducer).sendEmailVerification(any(EmailVerificationEvent.class));
    }

    @Test
    void resendVerification_shouldThrow_whenUserNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resendVerification("unknown@example.com"))
            .isInstanceOf(UserError.class)
            .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void resendVerification_shouldThrow_whenAlreadyVerified() {
        when(userRepository.findByEmail("active@example.com")).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> authService.resendVerification("active@example.com"))
            .isInstanceOf(UserError.class)
            .hasMessageContaining(ErrorCode.ALREADY_VERIFIED.getMessage());
    }

    // ==================== refresh ====================

    @Test
    void refresh_shouldReturnNewTokens_whenTokenValid() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(activeUser);
        refreshToken.setRevoked(false);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));

        Authentication auth = new UsernamePasswordAuthenticationToken(
            "active@example.com", null,
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );

        when(refreshTokenRepository.findByToken("valid-refresh")).thenReturn(Optional.of(refreshToken));
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("new-refresh-token");
        when(jwtProperties.getRefreshTokenExpiration()).thenReturn(604800000L);

        AuthResult result = authService.refresh("valid-refresh");

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(refreshToken.isRevoked()).isTrue();
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void refresh_shouldThrow_whenTokenNotFound() {
        when(refreshTokenRepository.findByToken("invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("invalid"))
            .isInstanceOf(UserError.class)
            .hasMessageContaining(ErrorCode.REFRESH_TOKEN_NOT_FOUND.getMessage());
    }

    @Test
    void refresh_shouldRevokeAllTokens_whenTokenReused() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(activeUser);
        refreshToken.setRevoked(true);

        when(refreshTokenRepository.findByToken("reused-token")).thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> authService.refresh("reused-token"))
            .isInstanceOf(UserError.class)
            .hasMessageContaining(ErrorCode.REFRESH_TOKEN_REUSED.getMessage());

        verify(refreshTokenRepository).revokeAllByUserId(activeUser.getId());
    }

    @Test
    void refresh_shouldThrow_whenTokenExpired() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(activeUser);
        refreshToken.setRevoked(false);
        refreshToken.setExpiresAt(LocalDateTime.now().minusDays(1));

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> authService.refresh("expired-token"))
            .isInstanceOf(UserError.class)
            .hasMessageContaining(ErrorCode.REFRESH_TOKEN_EXPIRED.getMessage());
    }

    // ==================== logout ====================

    @Test
    void logout_shouldBlacklistAccessTokenAndRevokeRefreshToken() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setRevoked(false);

        when(jwtTokenProvider.getJtiFromToken("access-token")).thenReturn("jti-123");
        when(jwtTokenProvider.getRemainingExpiry("access-token")).thenReturn(3600L);
        when(jwtTokenProvider.getEmailFromToken("access-token")).thenReturn("user@example.com");
        when(refreshTokenRepository.findByTokenAndRevokedFalse("refresh-token"))
            .thenReturn(Optional.of(refreshToken));

        authService.logout("access-token", "refresh-token");

        verify(tokenBlacklistService).blacklist("jti-123", "user@example.com", 3600L);
        assertThat(refreshToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(refreshToken);
    }

    @Test
    void logout_shouldOnlyBlacklistAccessToken_whenRefreshTokenIsNull() {
        when(jwtTokenProvider.getJtiFromToken("access-token")).thenReturn("jti-123");
        when(jwtTokenProvider.getRemainingExpiry("access-token")).thenReturn(3600L);
        when(jwtTokenProvider.getEmailFromToken("access-token")).thenReturn("user@example.com");

        authService.logout("access-token", null);

        verify(tokenBlacklistService).blacklist("jti-123", "user@example.com", 3600L);
        verify(refreshTokenRepository, never()).findByTokenAndRevokedFalse(any());
    }

    @Test
    void logout_shouldOnlyRevokeRefreshToken_whenAccessTokenIsNull() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setRevoked(false);

        when(refreshTokenRepository.findByTokenAndRevokedFalse("refresh-token"))
            .thenReturn(Optional.of(refreshToken));

        authService.logout(null, "refresh-token");

        verify(tokenBlacklistService, never()).blacklist(any(), any(), anyLong());
        assertThat(refreshToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(refreshToken);
    }

    @Test
    void logout_shouldDoNothing_whenRefreshTokenNotFound() {
        when(jwtTokenProvider.getJtiFromToken("access-token")).thenReturn("jti-123");
        when(jwtTokenProvider.getRemainingExpiry("access-token")).thenReturn(3600L);
        when(jwtTokenProvider.getEmailFromToken("access-token")).thenReturn("user@example.com");
        when(refreshTokenRepository.findByTokenAndRevokedFalse("invalid-token"))
            .thenReturn(Optional.empty());

        authService.logout("access-token", "invalid-token");

        verify(tokenBlacklistService).blacklist("jti-123", "user@example.com", 3600L);
        verify(refreshTokenRepository, never()).save(any());
    }
}