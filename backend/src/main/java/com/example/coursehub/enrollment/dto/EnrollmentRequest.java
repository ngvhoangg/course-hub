package com.example.coursehub.enrollment.dto;

import jakarta.validation.constraints.NotNull;

public record EnrollmentRequest(
    @NotNull(message = "User id is required")
    Long userId,

    @NotNull(message = "Course id is required")
    Long courseId
) {}
