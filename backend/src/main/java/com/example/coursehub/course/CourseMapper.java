package com.example.coursehub.course;

import com.example.coursehub.category.Category;
import com.example.coursehub.course.dto.CourseListResponse;
import com.example.coursehub.lesson.Lesson;
import com.example.coursehub.course.dto.CourseDetailResponse;
import com.example.coursehub.lesson.dto.LessonResponse;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {
    public CourseDetailResponse toDetailResponse(Course course) {
        return new CourseDetailResponse(
            course.getId(),
            course.getTitle(),
            course.getDescription(),
            course.getPrice(),
            course.getImageUrl(),
            course.getStatus(),
            course.getInstructor().getFullName(),
            course.getCategories()
                .stream()
                .map(Category::getName)
                .toList(),
            course.getLessons()
                .stream()
                .map(this::toLessonResponse)
                .toList()
        );
    }

    public CourseListResponse toListResponse(Course course) {
        return new CourseListResponse(
            course.getId(),
            course.getTitle(),
            course.getPrice(),
            course.getImageUrl()
        );
    }

    private LessonResponse toLessonResponse(Lesson lesson) {
        return new LessonResponse(
            lesson.getId(),
            lesson.getTitle(),
            lesson.getOrderIndex()
        );
    }
}
