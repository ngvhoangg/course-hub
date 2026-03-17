package com.example.coursehub.lesson.dto;

public record LessonResponse (
    Long id,
    String title,
    Integer orderIndex
) {}
