package com.example.coursehub.common.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitServiceTest {

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService();
    }

    @Test
    void tryConsume_shouldReturnTrue_whenWithinLimit() {
        boolean result = rateLimitService.tryConsume("test-key", 5, Duration.ofMinutes(1));
        assertThat(result).isTrue();
    }

    @Test
    void tryConsume_shouldReturnFalse_whenLimitExceeded() {
        for (int i = 0; i < 3; i++) {
            rateLimitService.tryConsume("test-key", 3, Duration.ofMinutes(1));
        }
        boolean result = rateLimitService.tryConsume("test-key", 3, Duration.ofMinutes(1));
        assertThat(result).isFalse();
    }

    @Test
    void tryConsume_shouldTrackSeparately_forDifferentKeys() {
        for (int i = 0; i < 3; i++) {
            rateLimitService.tryConsume("key-a", 3, Duration.ofMinutes(1));
        }

        boolean keyAExhausted = rateLimitService.tryConsume("key-a", 3, Duration.ofMinutes(1));
        boolean keyBAllowed = rateLimitService.tryConsume("key-b", 3, Duration.ofMinutes(1));

        assertThat(keyAExhausted).isFalse();
        assertThat(keyBAllowed).isTrue();
    }

    @Test
    void tryConsume_shouldUseSameBucket_forSameKey() {
        rateLimitService.tryConsume("same-key", 2, Duration.ofMinutes(1));
        rateLimitService.tryConsume("same-key", 2, Duration.ofMinutes(1));

        boolean result = rateLimitService.tryConsume("same-key", 2, Duration.ofMinutes(1));
        assertThat(result).isFalse();
    }

    @Test
    void resolveBucket_shouldIgnoreNewCapacity_ifBucketAlreadyExists() {

        rateLimitService.tryConsume("key", 2, Duration.ofMinutes(1));
        rateLimitService.tryConsume("key", 2, Duration.ofMinutes(1));

        boolean result = rateLimitService.tryConsume("key", 10, Duration.ofMinutes(1));

        assertThat(result).isFalse();
    }

    @Test
    void resolveBucket_shouldReturnSameBucket_forSameKey() {
        var bucket1 = rateLimitService.resolveBucket("same-key", 5, Duration.ofMinutes(1));
        var bucket2 = rateLimitService.resolveBucket("same-key", 5, Duration.ofMinutes(1));
        assertThat(bucket1).isSameAs(bucket2);
    }

    @Test
    void resolveBucket_shouldReturnDifferentBuckets_forDifferentKeys() {
        var bucket1 = rateLimitService.resolveBucket("key-1", 5, Duration.ofMinutes(1));
        var bucket2 = rateLimitService.resolveBucket("key-2", 5, Duration.ofMinutes(1));
        assertThat(bucket1).isNotSameAs(bucket2);
    }
}