package com.example.coursehub.review.dto;

import jakarta.validation.constraints.*;

public record ReviewRequest(
    @NotNull(message = "User id is required")
    Long userId,

    @NotNull(message = "Course id is required")
    Long courseId,

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must not exceed 5")
    Integer rating,

    @NotBlank(message = "Review comment is required")
    @Size(max = 1000, message = "Review comment must not exceed 1000 characters")
    String comment
) {}
