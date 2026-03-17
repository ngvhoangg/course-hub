package com.example.coursehub.lesson.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record LessonRequest (
    @NotBlank(message = "Lesson title is required")
    String title,

    @NotBlank(message = "Lesson content is required")
    String content,

    @NotNull(message = "Order index is required")
    @Positive(message = "Order index must be a positive number")
    Integer orderIndex
) {}
