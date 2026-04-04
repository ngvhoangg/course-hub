package com.example.coursehub.course;

import com.example.coursehub.category.Category;
import com.example.coursehub.category.CategoryRepository;
import com.example.coursehub.lesson.Lesson;
import com.example.coursehub.user.Role;
import com.example.coursehub.user.User;
import com.example.coursehub.user.UserRepository;
import com.example.coursehub.user.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CourseRepositoryTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired private CourseRepository courseRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;

    private User instructor;
    private Course course;
    private Category category;

    @BeforeEach
    void setUp() {
        instructor = new User();
        instructor.setEmail("instructor@example.com");
        instructor.setPasswordHash("hashed");
        instructor.setFullName("Instructor");
        instructor.setRole(Role.INSTRUCTOR);
        instructor.setStatus(UserStatus.ACTIVE);
        instructor.setCreatedAt(LocalDateTime.now());
        userRepository.save(instructor);

        category = new Category();
        category.setName("Programming");
        categoryRepository.save(category);

        course = new Course();
        course.setTitle("Spring Boot Course");
        course.setDescription("Learn Spring Boot");
        course.setPrice(new BigDecimal("99.99"));
        course.setStatus(CourseStatus.PUBLISHED);
        course.setImageUrl("http://example.com/image.jpg");
        course.setInstructor(instructor);
        course.setCreated_at(LocalDateTime.now());
        courseRepository.save(course);
    }

    // sanity test
    @Test
    void findByIdWithDetails_shouldReturnCourse_whenExists() {
        Optional<Course> result = courseRepository.findByIdWithDetails(course.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getInstructor()).isNotNull();
        assertThat(result.get().getTitle()).isEqualTo("Spring Boot Course");
    }

    // fetch relations
    @Test
    void findByIdWithDetails_shouldFetchAllRelations() {
        course.getCategories().add(category);

        Lesson lesson = new Lesson();
        lesson.setTitle("Lesson 1");
        lesson.setContent("Content");
        lesson.setOrderIndex(1);
        lesson.setCourse(course);

        course.getLessons().add(lesson);

        courseRepository.save(course);

        Course result = courseRepository.findByIdWithDetails(course.getId())
            .orElseThrow();

        assertThat(result.getInstructor()).isNotNull();
        assertThat(result.getCategories()).hasSize(1);
        assertThat(result.getLessons()).hasSize(1);
    }

    // critical test: detect duplicate
    @Test
    void findByIdWithDetails_shouldNotDuplicateLessons_whenMultipleCategoriesAndLessons() {
        Category c1 = new Category();
        c1.setName("Cat1");
        categoryRepository.save(c1);

        Category c2 = new Category();
        c2.setName("Cat2");
        categoryRepository.save(c2);

        course.getCategories().add(c1);
        course.getCategories().add(c2);

        Lesson l1 = new Lesson();
        l1.setTitle("L1");
        l1.setContent("C1");
        l1.setOrderIndex(1);
        l1.setCourse(course);

        Lesson l2 = new Lesson();
        l2.setTitle("L2");
        l2.setContent("C2");
        l2.setOrderIndex(2);
        l2.setCourse(course);

        course.getLessons().add(l1);
        course.getLessons().add(l2);

        courseRepository.save(course);

        Course result = courseRepository.findByIdWithDetails(course.getId())
            .orElseThrow();

        // detect duplicate lessons
        assertThat(result.getLessons())
            .extracting(Lesson::getId)
            .doesNotHaveDuplicates();

        assertThat(result.getLessons()).hasSize(2);
    }

    // not found
    @Test
    void findByIdWithDetails_shouldReturnEmpty_whenNotExists() {
        assertThat(courseRepository.findByIdWithDetails(999L)).isEmpty();
    }

    @Test
    void searchByKeyword_shouldReturnMatchingCoursesOrderedByRelevance() {
        Course titleMatch = new Course();
        titleMatch.setTitle("UniqueSearchToken Course");
        titleMatch.setDescription("Description with UniqueSearchToken");
        titleMatch.setPrice(new BigDecimal("19.99"));
        titleMatch.setStatus(CourseStatus.PUBLISHED);
        titleMatch.setImageUrl("http://example.com/image1.jpg");
        titleMatch.setInstructor(instructor);
        titleMatch.setCreated_at(LocalDateTime.now());
        courseRepository.save(titleMatch);

        Course descriptionMatch = new Course();
        descriptionMatch.setTitle("Another course");
        descriptionMatch.setDescription("Only description has UniqueSearchToken");
        descriptionMatch.setPrice(new BigDecimal("29.99"));
        descriptionMatch.setStatus(CourseStatus.PUBLISHED);
        descriptionMatch.setImageUrl("http://example.com/image2.jpg");
        descriptionMatch.setInstructor(instructor);
        descriptionMatch.setCreated_at(LocalDateTime.now());
        courseRepository.save(descriptionMatch);

        Page<Course> result = courseRepository.searchByKeyword(
            "UniqueSearchToken",
            PageRequest.of(0, 10)
        );

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
            .extracting(Course::getId)
            .contains(titleMatch.getId(), descriptionMatch.getId());

        // higher weight for title match should make it first
        assertThat(result.getContent().get(0).getId()).isEqualTo(titleMatch.getId());
    }

    @Test
    void searchByKeyword_shouldReturnEmptyPage_whenNoCourseMatches() {
        Page<Course> result = courseRepository.searchByKeyword(
            "KeywordThatDoesNotExist12345",
            PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }
}