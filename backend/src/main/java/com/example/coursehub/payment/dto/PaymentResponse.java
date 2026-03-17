package com.example.coursehub.payment.dto;

import com.example.coursehub.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
    Long id,
    String courseTitle,
    BigDecimal amount,
    String provider,
    PaymentStatus status,
    LocalDateTime createdAt
) {}
