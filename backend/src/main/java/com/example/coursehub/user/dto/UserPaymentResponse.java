package com.example.coursehub.user.dto;

import com.example.coursehub.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserPaymentResponse (
    Long paymentId,
    String courseTitle,
    BigDecimal amount,
    PaymentStatus status,
    LocalDateTime createdAt
) {}
