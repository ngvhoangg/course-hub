package com.example.coursehub.course.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record UpdateCourseRequest(
    @Size(max = 200, message = "Title must not exceed 200 characters")
    String title,

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    String description,

    @Positive(message = "Price must be greater than 0")
    BigDecimal price,

    String imageUrl,

    List<Long> categoryIds
) {}

