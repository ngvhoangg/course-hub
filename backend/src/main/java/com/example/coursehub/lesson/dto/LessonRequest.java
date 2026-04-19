package com.example.coursehub.lesson.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record LessonRequest (
    @Size(max = 500, message = "Title must not exceed 500 characters")
    String title,

    String content,

    @Positive(message = "Order index must be a positive number")
    Integer orderIndex
) {}
