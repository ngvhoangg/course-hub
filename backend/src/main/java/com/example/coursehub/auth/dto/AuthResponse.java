package com.example.coursehub.auth.dto;

public record AuthResponse(
    String accessToken,
    String sessionId
) {}
