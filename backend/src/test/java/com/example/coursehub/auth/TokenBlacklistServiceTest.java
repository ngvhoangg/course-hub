package com.example.coursehub.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    private String testJti;
    private String testEmail;
    private long testTtl;

    @BeforeEach
    void setUp() {
        testJti = UUID.randomUUID().toString();
        testEmail = "user@example.com";
        testTtl = 900000L; // 15 minutes

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ==================== blacklist ====================

    @Test
    void blacklist_shouldStoreTokenInRedis() {
        tokenBlacklistService.blacklist(testJti, testEmail, testTtl);

        verify(valueOperations).set(
            "blacklist:" + testJti,
            testEmail,
            testTtl,
            TimeUnit.MILLISECONDS
        );
    }

    @Test
    void blacklist_shouldUseCorrectKeyPrefix() {
        tokenBlacklistService.blacklist(testJti, testEmail, testTtl);

        verify(valueOperations).set(
            startsWith("blacklist:"),
            eq(testEmail),
            eq(testTtl),
            eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    void blacklist_shouldStoreEmailAsValue() {
        tokenBlacklistService.blacklist(testJti, testEmail, testTtl);

        verify(valueOperations).set(
            anyString(),
            eq(testEmail),
            anyLong(),
            any(TimeUnit.class)
        );
    }

    @Test
    void blacklist_shouldUseTtlInMilliseconds() {
        tokenBlacklistService.blacklist(testJti, testEmail, testTtl);

        verify(valueOperations).set(
            anyString(),
            anyString(),
            eq(testTtl),
            eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    void blacklist_shouldNotStoreToken_whenTtlIsZero() {
        tokenBlacklistService.blacklist(testJti, testEmail, 0);

        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void blacklist_shouldNotStoreToken_whenTtlIsNegative() {
        tokenBlacklistService.blacklist(testJti, testEmail, -1000L);

        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    // ==================== isBlacklisted ====================

    @Test
    void isBlacklisted_shouldReturnTrue_whenTokenIsBlacklisted() {
        when(redisTemplate.hasKey("blacklist:" + testJti)).thenReturn(true);

        assertThat(tokenBlacklistService.isBlacklisted(testJti)).isTrue();
    }

    @Test
    void isBlacklisted_shouldReturnFalse_whenTokenIsNotBlacklisted() {
        when(redisTemplate.hasKey("blacklist:" + testJti)).thenReturn(false);

        assertThat(tokenBlacklistService.isBlacklisted(testJti)).isFalse();
    }

    @Test
    void isBlacklisted_shouldReturnFalse_whenKeyIsNull() {
        when(redisTemplate.hasKey("blacklist:" + testJti)).thenReturn(null);

        assertThat(tokenBlacklistService.isBlacklisted(testJti)).isFalse();
    }

    @Test
    void isBlacklisted_shouldUseCorrectKeyFormat() {
        tokenBlacklistService.isBlacklisted(testJti);

        verify(redisTemplate).hasKey("blacklist:" + testJti);
    }

    // ==================== Integration scenarios ====================

    @Test
    void shouldBlacklistAndCheckBlacklist() {
        when(redisTemplate.hasKey("blacklist:" + testJti)).thenReturn(false);

        assertThat(tokenBlacklistService.isBlacklisted(testJti)).isFalse();

        tokenBlacklistService.blacklist(testJti, testEmail, testTtl);

        when(redisTemplate.hasKey("blacklist:" + testJti)).thenReturn(true);

        assertThat(tokenBlacklistService.isBlacklisted(testJti)).isTrue();
    }

    @Test
    void blacklist_shouldHandleMultipleDifferentTokens() {
        String jti1 = UUID.randomUUID().toString();
        String jti2 = UUID.randomUUID().toString();

        tokenBlacklistService.blacklist(jti1, "user1@example.com", testTtl);
        tokenBlacklistService.blacklist(jti2, "user2@example.com", testTtl);

        verify(valueOperations, times(2)).set(
            anyString(),
            anyString(),
            eq(testTtl),
            eq(TimeUnit.MILLISECONDS)
        );
    }
}
