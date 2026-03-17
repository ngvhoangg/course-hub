package com.example.coursehub.course.dto;

import java.math.BigDecimal;

public record CourseListResponse (
    Long id,
    String title,
    BigDecimal price,
    String imageUrl
) {}
