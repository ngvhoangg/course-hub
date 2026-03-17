package com.example.coursehub.payment.dto;

import jakarta.validation.constraints.NotNull;

public record PaymentRequest(
    @NotNull(message = "User id is required")
    Long userId,

    @NotNull(message = "Course id is required")
    Long courseId,

    @NotNull(message = "Payment provider is required")
    String provider
) {}
