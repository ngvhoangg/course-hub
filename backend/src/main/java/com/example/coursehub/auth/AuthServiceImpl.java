package com.example.coursehub.auth;

import com.example.coursehub.auth.dto.AuthResult;
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

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider, EmailVerificationTokenRepository emailVerificationTokenRepository, RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties, EventProducer eventProducer, TokenBlacklistService tokenBlacklistService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
        this.eventProducer = eventProducer;
        this.tokenBlacklistService = tokenBlacklistService;
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
    public AuthResult login(String email, String password) {
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

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshTokenString = jwtTokenProvider.generateRefreshToken(authentication);

        // save new refresh token
        createAndSaveRefreshToken(user, refreshTokenString);

        return new AuthResult(accessToken, refreshTokenString);
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

        token.setRevoked(true);
        refreshTokenRepository.save(token);

        User user = token.getUser();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            user.getEmail(), null,
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );

        String newRefreshToken = jwtTokenProvider.generateRefreshToken(authentication);
        String newAccessToken = jwtTokenProvider.generateAccessToken(authentication);

        // save new refresh token
        createAndSaveRefreshToken(user, newRefreshToken);

        return new AuthResult(newAccessToken, newRefreshToken);
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

    private void createAndSaveRefreshToken(User user, String tokenString) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(tokenString);
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
