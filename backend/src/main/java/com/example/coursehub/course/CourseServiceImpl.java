package com.example.coursehub.course;

import com.example.coursehub.ai.embedding.EntityType;
import com.example.coursehub.category.Category;
import com.example.coursehub.common.kafka.event.EntityAction;
import com.example.coursehub.common.kafka.event.EntitySyncEvent;
import com.example.coursehub.common.kafka.producer.EventProducer;
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
import java.util.*;

@Service
public class CourseServiceImpl implements CourseService {
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CourseMapper courseMapper;
    private final EventProducer eventProducer;

    public CourseServiceImpl(CourseRepository courseRepository, UserRepository userRepository, CategoryRepository categoryRepository, CourseMapper courseMapper, EventProducer eventProducer) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.courseMapper = courseMapper;
        this.eventProducer = eventProducer;
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
        sendUpsertEvent(savedCourse, true);
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

        boolean contentChanged = (request.title() != null && !request.title().equals(course.getTitle()))
            || (request.description() != null && !request.description().equals(course.getDescription()));

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

        sendUpsertEvent(course, contentChanged);
        return courseMapper.toDetailResponse(course);
    }

    @Override
    @Transactional
    public void deleteCourse(Long id) {
        Course course = courseRepository.findById(id)
            .orElseThrow(() -> new UserError(ErrorCode.COURSE_NOT_FOUND));

        courseRepository.delete(course);
        sendDeleteEvent(id);
    }

    @Override
    public Page<CourseListResponse> searchCourses(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return courseRepository.findAll(pageable).map(courseMapper::toListResponse);
        }

        return courseRepository.searchByKeyword(query, pageable)
            .map(courseMapper::toListResponse);
    }

    // sync for creating, updating course
    private void sendUpsertEvent(Course course, boolean reEmbed) {
        Map<String, Object> metadata = new HashMap<>();
        if (course.getPrice() != null) {
            metadata.put("price", course.getPrice());
        }
        if (course.getCategories() != null) {
            metadata.put("category_ids",
                course.getCategories().stream().map(Category::getId).toList());
        }

        EntitySyncEvent event = new EntitySyncEvent(
            course.getId(),
            EntityType.COURSE,
            EntityAction.UPSERT,
            metadata,
            reEmbed,
            System.currentTimeMillis()
        );

        eventProducer.sendEntitySyncEvent(event);
    }

    // sync for deleting course
    private void sendDeleteEvent(Long id) {
        EntitySyncEvent event = new EntitySyncEvent(
            id,
            EntityType.COURSE,
            EntityAction.DELETE,
            Map.of(),
            false,
            System.currentTimeMillis()
        );
        eventProducer.sendEntitySyncEvent(event);
    }
}
