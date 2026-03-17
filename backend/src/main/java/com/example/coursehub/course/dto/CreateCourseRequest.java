package com.example.coursehub.course.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record CreateCourseRequest(
    @NotBlank(message = "Course title is required")
    @Size(max = 200, message = "Course title must not exceed 200 characters")
    String title,

    @NotBlank(message = "Course description is required")
    @Size(max = 2000, message = "Course description must not exceed 2000 characters")
    String description,

    @NotNull(message = "Course price is required")
    @Positive(message = "Course price must be greater than 0")
    BigDecimal price,

    @NotBlank(message = "Course image URL is required")
    @Pattern(
        regexp = "^(http|https)://.*$",
        message = "Image URL must be a valid HTTP or HTTPS URL"
    )
    String imageUrl,

    @NotEmpty(message = "At least one category is required")
    List<@NotNull(message = "Category id must not be null") Long> categoryIds
) {}
