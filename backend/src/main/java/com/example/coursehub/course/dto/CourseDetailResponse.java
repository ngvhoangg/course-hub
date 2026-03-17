package com.example.coursehub.course.dto;

import com.example.coursehub.course.CourseStatus;
import com.example.coursehub.lesson.dto.LessonResponse;

import java.math.BigDecimal;
import java.util.List;

public record CourseDetailResponse (
    Long id,
    String title,
    String description,
    BigDecimal price,
    String imageUrl,
    CourseStatus status,
    String instructorName,
    List<String> categories,
    List<LessonResponse> lessons
) {}
