package com.example.coursehub.enrollment;

import com.example.coursehub.course.Course;
import com.example.coursehub.course.CourseStatus;
import com.example.coursehub.course.CourseRepository;
import com.example.coursehub.user.Role;
import com.example.coursehub.user.User;
import com.example.coursehub.user.UserRepository;
import com.example.coursehub.user.UserStatus;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EnrollmentRepositoryTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;

    private User student;
    private User anotherStudent;
    private Course course1;
    private Course course2;
    private User instructor;

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

        student = new User();
        student.setEmail("student@example.com");
        student.setPasswordHash("hashed");
        student.setFullName("Student");
        student.setRole(Role.STUDENT);
        student.setStatus(UserStatus.ACTIVE);
        student.setCreatedAt(LocalDateTime.now());
        userRepository.save(student);

        anotherStudent = new User();
        anotherStudent.setEmail("another@example.com");
        anotherStudent.setPasswordHash("hashed");
        anotherStudent.setFullName("Another Student");
        anotherStudent.setRole(Role.STUDENT);
        anotherStudent.setStatus(UserStatus.ACTIVE);
        anotherStudent.setCreatedAt(LocalDateTime.now());
        userRepository.save(anotherStudent);

        course1 = new Course();
        course1.setTitle("Course 1");
        course1.setDescription("Description 1");
        course1.setPrice(new BigDecimal("99.99"));
        course1.setStatus(CourseStatus.PUBLISHED);
        course1.setImageUrl("http://example.com/image1.jpg");
        course1.setInstructor(instructor);
        course1.setCreated_at(LocalDateTime.now());
        courseRepository.save(course1);

        course2 = new Course();
        course2.setTitle("Course 2");
        course2.setDescription("Description 2");
        course2.setPrice(new BigDecimal("49.99"));
        course2.setStatus(CourseStatus.PUBLISHED);
        course2.setImageUrl("http://example.com/image2.jpg");
        course2.setInstructor(instructor);
        course2.setCreated_at(LocalDateTime.now());
        courseRepository.save(course2);
    }

    private Enrollment createEnrollment(User user, Course course) {
        Enrollment enrollment = new Enrollment();
        enrollment.setUser(user);
        enrollment.setCourse(course);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollment.setProgress(0);
        return enrollmentRepository.save(enrollment);
    }

    // findByUserIdWithCourse
    @Test
    void findByUserIdWithCourse_shouldReturnEnrollmentsWithFetchedCourse() {
        createEnrollment(student, course1);
        createEnrollment(student, course2);

        List<Enrollment> result = enrollmentRepository.findByUserIdWithCourse(student.getId());

        assertThat(result).hasSize(2);

        assertThat(result)
            .extracting(e -> e.getCourse().getTitle())
            .containsExactlyInAnyOrder("Course 1", "Course 2");
    }

    @Test
    void findByUserIdWithCourse_shouldReturnEmpty_whenNoEnrollments() {
        List<Enrollment> result = enrollmentRepository.findByUserIdWithCourse(student.getId());
        assertThat(result).isEmpty();
    }

    @Test
    void findByUserIdWithCourse_shouldReturnOnlyEnrollmentsOfThatUser() {
        createEnrollment(student, course1);
        createEnrollment(anotherStudent, course2);

        List<Enrollment> result = enrollmentRepository.findByUserIdWithCourse(student.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCourse().getTitle()).isEqualTo("Course 1");
    }
}