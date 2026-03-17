package com.example.coursehub.common.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String key, int capacity, Duration duration) {
        return buckets.computeIfAbsent(key, k -> createBucket(capacity, duration));
    }

    private Bucket createBucket(int capacity, Duration duration) {
        Bandwidth limit = Bandwidth.builder()
            .capacity(capacity)
            .refillGreedy(capacity, duration)
            .build();
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    public boolean tryConsume(String key, int capacity, Duration duration) {
        return resolveBucket(key, capacity, duration).tryConsume(1);
    }
}
