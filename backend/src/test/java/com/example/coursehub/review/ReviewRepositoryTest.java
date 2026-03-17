package com.example.coursehub.review;

import com.example.coursehub.course.Course;
import com.example.coursehub.course.CourseRepository;
import com.example.coursehub.course.CourseStatus;
import com.example.coursehub.user.Role;
import com.example.coursehub.user.User;
import com.example.coursehub.user.UserRepository;
import com.example.coursehub.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
class ReviewRepositoryTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired private ReviewRepository reviewRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;

    private User user;
    private User anotherUser;
    private User instructor;
    private Course course;

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

        user = new User();
        user.setEmail("user@example.com");
        user.setPasswordHash("hashed");
        user.setFullName("User");
        user.setRole(Role.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        anotherUser = new User();
        anotherUser.setEmail("another@example.com");
        anotherUser.setPasswordHash("hashed");
        anotherUser.setFullName("Another");
        anotherUser.setRole(Role.STUDENT);
        anotherUser.setStatus(UserStatus.ACTIVE);
        anotherUser.setCreatedAt(LocalDateTime.now());
        userRepository.save(anotherUser);

        course = new Course();
        course.setTitle("Course 1");
        course.setDescription("Desc");
        course.setPrice(new BigDecimal("100"));
        course.setStatus(CourseStatus.PUBLISHED);
        course.setImageUrl("img");
        course.setInstructor(instructor);
        course.setCreated_at(LocalDateTime.now());
        courseRepository.save(course);
    }

    private Review createReview(User user, Course course) {
        Review r = new Review();
        r.setUser(user);
        r.setCourse(course);
        r.setRating(5);
        r.setComment("Great!");
        r.setStatus(ReviewStatus.VISIBLE);
        r.setCreated_at(LocalDateTime.now());
        return reviewRepository.save(r);
    }

    // Fetch + data
    @Test
    void findByCourseIdWithUser_shouldReturnReviewsWithUser() {
        createReview(user, course);
        createReview(anotherUser, course);

        List<Review> result = reviewRepository.findByCourseIdWithUser(course.getId());

        assertThat(result).hasSize(2);

        assertThat(result)
            .extracting(r -> r.getUser().getEmail())
            .containsExactlyInAnyOrder("user@example.com", "another@example.com");
    }

    // Empty
    @Test
    void findByCourseIdWithUser_shouldReturnEmpty_whenNoReviews() {
        List<Review> result = reviewRepository.findByCourseIdWithUser(course.getId());

        assertThat(result).isEmpty();
    }

    // Ensure fetch user works
    @Test
    void findByCourseIdWithUser_shouldFetchUserDetails() {
        createReview(user, course);

        List<Review> result = reviewRepository.findByCourseIdWithUser(course.getId());

        assertThat(result.get(0).getUser().getFullName()).isEqualTo("User");
    }

    // Unique constraint
    @Test
    void shouldNotAllowDuplicateReviewPerUserAndCourse() {
        createReview(user, course);

        assertThatThrownBy(() -> createReview(user, course))
            .isInstanceOf(Exception.class); // hoặc DataIntegrityViolationException nếu muốn strict
    }
}