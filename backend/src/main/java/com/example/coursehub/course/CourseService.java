package com.example.coursehub.course;

import com.example.coursehub.course.dto.CourseDetailResponse;
import com.example.coursehub.course.dto.CourseListResponse;
import com.example.coursehub.course.dto.UpdateCourseRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface CourseService {
    Page<CourseListResponse> getCourses(Pageable pageable);
    CourseDetailResponse getCourseById(Long id);
    CourseDetailResponse createCourse(String title,
                                      String description,
                                      BigDecimal price,
                                      String imageUrl,
                                      List<Long> categoryIds);
    CourseDetailResponse updateCourse(Long id, UpdateCourseRequest request);
    void deleteCourse(Long id);
    Page<CourseListResponse> searchCourses(String query, Pageable pageable);
}
