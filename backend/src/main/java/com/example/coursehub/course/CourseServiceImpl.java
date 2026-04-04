package com.example.coursehub.course;

import com.example.coursehub.category.Category;
import com.example.coursehub.user.User;
import com.example.coursehub.course.dto.CourseDetailResponse;
import com.example.coursehub.course.dto.CourseListResponse;
import com.example.coursehub.course.dto.UpdateCourseRequest;
import com.example.coursehub.common.exception.ErrorCode;
import com.example.coursehub.common.exception.UserError;
import com.example.coursehub.category.CategoryRepository;
import com.example.coursehub.user.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

@Service
public class CourseServiceImpl implements CourseService {
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CourseMapper courseMapper;

    public CourseServiceImpl(CourseRepository courseRepository, UserRepository userRepository, CategoryRepository categoryRepository, CourseMapper courseMapper) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.courseMapper = courseMapper;
    }

    @Override
    public Page<CourseListResponse> getCourses(Pageable pageable) {
        return courseRepository.findAll(pageable)
            .map(course -> new CourseListResponse(
                course.getId(),
                course.getTitle(),
                course.getPrice(),
                course.getImageUrl()
            ));
    }

    @Override
    public CourseDetailResponse getCourseById(Long id) {
        Course course = courseRepository.findById(id)
            .orElseThrow(() -> new UserError(ErrorCode.COURSE_NOT_FOUND));

        return courseMapper.toDetailResponse(course);
    }

    @Override
    @Transactional
    public CourseDetailResponse createCourse(String title, String description, BigDecimal price, String imageUrl, List<Long> categoryIds) {
        User instructor = userRepository.findById(1L)
            .orElseThrow(() ->
                new UserError(ErrorCode.INSTRUCTOR_NOT_FOUND));

        Course course = new Course();
        course.setTitle(title);
        course.setDescription(description);
        course.setPrice(price);
        course.setImageUrl(imageUrl);
        course.setInstructor(instructor);
        course.setStatus(CourseStatus.DRAFT);
        course.setCreated_at(LocalDateTime.now());
        List<Category> categories = categoryRepository.findAllById(categoryIds);
        if (categories.size() != categoryIds.size()) {
            throw new UserError(ErrorCode.CATEGORY_NOT_FOUND);
        }
        course.setCategories(new HashSet<>(categories));

        Course savedCourse = courseRepository.save(course);
        return courseMapper.toDetailResponse(savedCourse);
    }

    @Override
    @Transactional
    public CourseDetailResponse updateCourse(Long id, UpdateCourseRequest request) {
        Course course =  courseRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new UserError(ErrorCode.COURSE_NOT_FOUND));

        if (request.title() == null &&
            request.description() == null &&
            request.price() == null &&
            request.imageUrl() == null &&
            request.categoryIds() == null)
        {
            throw new UserError(ErrorCode.EMPTY_UPDATE_REQUEST);
        }
        if (request.title() != null) {
            course.setTitle(request.title());
        }
        if (request.description() != null) {
            course.setDescription(request.description());
        }
        if (request.price() != null) {
            course.setPrice(request.price());
        }
        if (request.imageUrl() != null) {
            course.setImageUrl(request.imageUrl());
        }
        if (request.categoryIds() != null) {
            List<Category> categories =
                categoryRepository.findAllById(request.categoryIds());

            if (categories.size() != request.categoryIds().size()) {
                throw new UserError(ErrorCode.CATEGORY_NOT_FOUND);
            }
            course.setCategories(new HashSet<>(categories));
        }

        courseRepository.save(course);

        return courseMapper.toDetailResponse(course);
    }

    @Override
    @Transactional
    public void deleteCourse(Long id) {
        Course course = courseRepository.findById(id)
            .orElseThrow(() -> new UserError(ErrorCode.COURSE_NOT_FOUND));

        courseRepository.delete(course);
    }

    @Override
    public Page<CourseListResponse> searchCourses(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return courseRepository.findAll(pageable).map(courseMapper::toListResponse);
        }

        return courseRepository.searchByKeyword(query, pageable)
            .map(courseMapper::toListResponse);
    }
}
