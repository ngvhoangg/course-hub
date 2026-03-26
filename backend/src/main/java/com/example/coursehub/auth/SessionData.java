package com.example.coursehub.auth;

import java.time.LocalDateTime;

public record SessionData(
    Long userId,
    String ip,
    String userAgent,
    LocalDateTime createdAt,
    LocalDateTime lastUsedAt
) {}
