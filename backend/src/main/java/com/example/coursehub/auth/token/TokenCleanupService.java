package com.example.coursehub.auth.token;

import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class TokenCleanupService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;

    public TokenCleanupService(RefreshTokenRepository refreshTokenRepository, EmailVerificationTokenRepository emailVerificationTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
    }

    @Scheduled(cron = "0 0 * * * *") // every hour
    @Transactional
    public void cleanupExpiredRefreshTokens() {
        int deleted = refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("Cleaned up {} expired refresh tokens", deleted);
    }

    @Scheduled(cron = "0 0 0 * * *") // every day at midnight
    @Transactional
    public void cleanupExpiredVerificationTokens() {
        int deleted = emailVerificationTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("Cleaned up {} expired verification tokens", deleted);
    }
}
