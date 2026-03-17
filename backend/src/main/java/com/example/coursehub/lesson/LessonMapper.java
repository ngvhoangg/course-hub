package com.example.coursehub.lesson;

import com.example.coursehub.lesson.dto.LessonResponse;
import org.springframework.stereotype.Component;

@Component
public class LessonMapper {
    public LessonResponse toLessonResponse(Lesson lesson) {
        return new LessonResponse(
            lesson.getId(),
            lesson.getTitle(),
            lesson.getOrderIndex()
        );
    }
}
