package com.example.coursehub.auth.dto;

public record AuthResult(
    String accessToken,
    String refreshToken
) {}
