package com.example.coursehub.auth.dto;

import java.time.LocalDateTime;

public record SessionResponse(
    String sessionId,
    String ip,
    String userAgent,
    LocalDateTime createdAt,
    LocalDateTime lastUsedAt
) {}
