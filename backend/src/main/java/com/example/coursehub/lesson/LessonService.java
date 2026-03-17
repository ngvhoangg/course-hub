package com.example.coursehub.lesson;

import com.example.coursehub.lesson.dto.LessonResponse;

import java.util.List;

public interface LessonService {
    List<LessonResponse> getAllLessons(Long courseId);
    LessonResponse createLesson(Long courseId, String title, String content, Integer orderIndex);
    void updateLesson(Long id, String title, String content, Integer orderIndex);
    void deleteLesson(Long id);
}
