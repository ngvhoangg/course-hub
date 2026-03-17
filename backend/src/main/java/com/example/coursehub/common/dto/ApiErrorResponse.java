package com.example.coursehub.common.dto;

public record ApiErrorResponse(
    String message,
    String errorCode,
    int status,
    long timestamp
) {}
