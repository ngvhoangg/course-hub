package com.example.coursehub.course;

import com.example.coursehub.course.dto.CourseDetailResponse;
import com.example.coursehub.course.dto.CourseListResponse;
import com.example.coursehub.course.dto.CreateCourseRequest;
import com.example.coursehub.course.dto.UpdateCourseRequest;
import com.example.coursehub.lesson.dto.LessonRequest;
import com.example.coursehub.lesson.dto.LessonResponse;
import com.example.coursehub.review.dto.ReviewResponse;
import com.example.coursehub.lesson.LessonService;
import com.example.coursehub.review.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    private final CourseService courseService;
    private final ReviewService reviewService;
    private final LessonService lessonService;

    public CourseController(CourseService courseService, ReviewService reviewService, LessonService lessonService) {
        this.courseService = courseService;
        this.reviewService = reviewService;
        this.lessonService = lessonService;
    }

    @GetMapping
    public Page<CourseListResponse> getCourses(Pageable pageable) {
        return courseService.getCourses(pageable);
    }

    @GetMapping("/{id}")
    public CourseDetailResponse getCourse(@PathVariable Long id) {
        return courseService.getCourseById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public CourseDetailResponse createCourse(@Valid @RequestBody CreateCourseRequest request) {
        return courseService.createCourse(request.title(),
            request.description(), request.price(), request.imageUrl(), request.categoryIds());
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public CourseDetailResponse updateCourse(
        @PathVariable Long id,
        @Valid @RequestBody UpdateCourseRequest request)
    {
        return courseService.updateCourse(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public void deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
    }

    @GetMapping("{id}/reviews")
    public List<ReviewResponse> getReviews(@PathVariable Long id) {
        return reviewService.getCourseReviews(id);
    }

    @GetMapping("/{id}/lessons")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN') or hasRole('STUDENT')")
    public List<LessonResponse> getLessons(@PathVariable Long id) {
        return lessonService.getAllLessons(id);
    }

    @PostMapping("/{id}/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public LessonResponse createLesson(@PathVariable Long id, @Valid @RequestBody LessonRequest request) {
        return lessonService.createLesson(id, request.title(), request.content(), request.orderIndex());
    }

    @GetMapping("/search")
    public Page<CourseListResponse> searchCourses(@RequestParam("q") String query, Pageable pageable) {
        return courseService.searchCourses(query, pageable);
    }
}
