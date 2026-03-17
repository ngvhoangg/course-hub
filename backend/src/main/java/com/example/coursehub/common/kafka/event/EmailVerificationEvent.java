package com.example.coursehub.common.kafka.event;

public record EmailVerificationEvent(
    String toEmail,
    String token
) {}
