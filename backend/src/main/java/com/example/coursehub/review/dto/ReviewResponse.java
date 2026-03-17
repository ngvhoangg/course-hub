package com.example.coursehub.review.dto;

import java.time.LocalDateTime;

public record ReviewResponse(
    Integer rating,
    String comment,
    String userName,
    LocalDateTime createdAt
) {}
