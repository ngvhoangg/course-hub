package com.example.coursehub.auth;

import com.example.coursehub.auth.dto.AuthResult;
import com.example.coursehub.auth.dto.SessionResponse;
import com.example.coursehub.auth.jwt.JwtProperties;
import com.example.coursehub.auth.jwt.JwtTokenProvider;
import com.example.coursehub.auth.token.*;
import com.example.coursehub.common.email.EmailService;
import com.example.coursehub.common.exception.ErrorCode;
import com.example.coursehub.common.exception.UserError;
import com.example.coursehub.common.kafka.event.EmailVerificationEvent;
import com.example.coursehub.common.kafka.producer.EventProducer;
import com.example.coursehub.user.Role;
import com.example.coursehub.user.User;
import com.example.coursehub.user.UserRepository;
import com.example.coursehub.user.UserStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final EventProducer eventProducer;
    private final TokenBlacklistService tokenBlacklistService;
    private final SessionService sessionService;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider, EmailVerificationTokenRepository emailVerificationTokenRepository, RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties, EventProducer eventProducer, TokenBlacklistService tokenBlacklistService, SessionService sessionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
        this.eventProducer = eventProducer;
        this.tokenBlacklistService = tokenBlacklistService;
        this.sessionService = sessionService;
    }

    @Override
    @Transactional
    public void register(String email, String password, String fullName) {
        if(userRepository.existsByEmail(email)) {
            throw new UserError(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setRole(Role.STUDENT);
        user.setStatus(UserStatus.PENDING);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        createAndSendVerificationToken(user);
    }

    @Override
    @Transactional
    public AuthResult login(String email, String password, String ip, String userAgent) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UserError(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.PENDING) {
            throw new UserError(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        if (user.getStatus() == UserStatus.DISABLED) {
            throw new UserError(ErrorCode.ACCOUNT_DISABLED);
        }

        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(email, password)
        );

        // create session
        String sessionId = sessionService.createSession(user.getId(), ip, userAgent);

        String accessToken = jwtTokenProvider.generateAccessToken(authentication, sessionId);
        String refreshTokenString = jwtTokenProvider.generateRefreshToken(authentication);

        // save new refresh token
        createAndSaveRefreshToken(user, refreshTokenString, sessionId);

        return new AuthResult(accessToken, refreshTokenString, sessionId);
    }

    @Override
    @Transactional
    public void resendVerification(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UserError(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new UserError(ErrorCode.ALREADY_VERIFIED);
        }

        emailVerificationTokenRepository.deleteByUserId(user.getId());

        createAndSendVerificationToken(user);
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(token)
            .orElseThrow(() -> new UserError(ErrorCode.VERIFICATION_TOKEN_NOT_FOUND));

        if (verificationToken.isUsed()) {
            throw new UserError(ErrorCode.VERIFICATION_TOKEN_ALREADY_USED);
        }

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UserError(ErrorCode.VERIFICATION_TOKEN_EXPIRED);
        }

        User user = verificationToken.getUser();
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        verificationToken.setUsed(true);
        emailVerificationTokenRepository.save(verificationToken);
    }

    @Override
    @Transactional(noRollbackFor = UserError.class)
    public AuthResult refresh(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
            .orElseThrow(() -> new UserError(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if (token.isRevoked()) {
            // possible token theft — revoke ALL tokens for this user
            refreshTokenRepository.revokeAllByUserId(token.getUser().getId());
            throw new UserError(ErrorCode.REFRESH_TOKEN_REUSED);
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UserError(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        // verify session exists in Redis
        String sessionId = token.getSessionId();
        if (sessionId == null || !sessionService.sessionExists(sessionId)) {
            throw new UserError(ErrorCode.SESSION_EXPIRED);
        }

        token.setRevoked(true);
        refreshTokenRepository.save(token);

        User user = token.getUser();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            user.getEmail(), null,
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );

        String newRefreshToken = jwtTokenProvider.generateRefreshToken(authentication);
        String newAccessToken = jwtTokenProvider.generateAccessToken(authentication, sessionId);

        // save new refresh token
        createAndSaveRefreshToken(user, newRefreshToken, sessionId);

        // update session lastUsedAt
        sessionService.updateLastUsedAt(sessionId, user.getId());

        return new AuthResult(newAccessToken, newRefreshToken, sessionId);
    }

    @Override
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        // blacklist access token
        if (accessToken != null) {
            String jti = jwtTokenProvider.getJtiFromToken(accessToken);
            long ttl = jwtTokenProvider.getRemainingExpiry(accessToken);
            String email = jwtTokenProvider.getEmailFromToken(accessToken);
            tokenBlacklistService.blacklist(jti, email, ttl);

            // delete session
            String sessionId = jwtTokenProvider.getSessionIdFromToken(accessToken);
            if (sessionId != null) {
                String userEmail = jwtTokenProvider.getEmailFromToken(accessToken);
                userRepository.findByEmail(userEmail).ifPresent(user ->
                    sessionService.deleteSession(user.getId(), sessionId)
                );
            }
        }

        // revoke refresh token
        if (refreshToken != null) {
            refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)
                .ifPresent(t -> {
                    t.setRevoked(true);
                    refreshTokenRepository.save(t);
                });
        }
    }

    @Override
    public List<SessionResponse> getSessions(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UserError(ErrorCode.USER_NOT_FOUND));
        return sessionService.getSessions(user.getId());
    }

    @Override
    @Transactional
    public void deleteSession(String email, String sessionId) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UserError(ErrorCode.USER_NOT_FOUND));

        sessionService.deleteSession(user.getId(), sessionId);
        refreshTokenRepository.findBySessionId(sessionId)
            .ifPresent(t -> {
                t.setRevoked(true);
                refreshTokenRepository.save(t);
            });
    }

    @Override
    @Transactional
    public void deleteAllSessions(String accessToken, String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UserError(ErrorCode.USER_NOT_FOUND));

        // blacklist current access token
        if (accessToken != null) {
            String jti = jwtTokenProvider.getJtiFromToken(accessToken);
            long ttl = jwtTokenProvider.getRemainingExpiry(accessToken);
            tokenBlacklistService.blacklist(jti, email, ttl);
        }

        // delete all sessions from Redis
        Set<String> sessionIds = sessionService.deleteAllSessions(user.getId());

        // revoke all refresh tokens in DB
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    private void createAndSaveRefreshToken(User user, String tokenString, String sessionId) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(tokenString);
        refreshToken.setSessionId(sessionId);
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(jwtProperties.getRefreshTokenExpiration() / 1000));
        refreshToken.setRevoked(false);
        refreshToken.setCreatedAt(LocalDateTime.now());

        refreshTokenRepository.save(refreshToken);
    }

    private void createAndSendVerificationToken(User user) {
        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setUser(user);
        verificationToken.setToken(token);
        verificationToken.setExpiresAt(LocalDateTime.now().plusHours(24));
        verificationToken.setUsed(false);
        verificationToken.setCreatedAt(LocalDateTime.now());
        emailVerificationTokenRepository.save(verificationToken);

        eventProducer.sendEmailVerification(new EmailVerificationEvent(user.getEmail(), token));
    }
}
