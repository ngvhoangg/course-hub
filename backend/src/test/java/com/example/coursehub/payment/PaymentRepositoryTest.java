package com.example.coursehub.payment;

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

@DataJpaTest
@Testcontainers
class PaymentRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired private PaymentRepository paymentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;

    private User user;
    private User anotherUser;
    private User instructor;
    private Course course1;
    private Course course2;

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
        anotherUser.setFullName("Another User");
        anotherUser.setRole(Role.STUDENT);
        anotherUser.setStatus(UserStatus.ACTIVE);
        anotherUser.setCreatedAt(LocalDateTime.now());
        userRepository.save(anotherUser);

        course1 = new Course();
        course1.setTitle("Course 1");
        course1.setDescription("Description 1");
        course1.setPrice(new BigDecimal("100.00"));
        course1.setStatus(CourseStatus.PUBLISHED);
        course1.setImageUrl("http://example.com/1.jpg");
        course1.setInstructor(instructor);
        course1.setCreated_at(LocalDateTime.now());
        courseRepository.save(course1);

        course2 = new Course();
        course2.setTitle("Course 2");
        course2.setDescription("Description 2");
        course2.setPrice(new BigDecimal("50.00"));
        course2.setStatus(CourseStatus.PUBLISHED);
        course2.setImageUrl("http://example.com/2.jpg");
        course2.setInstructor(instructor);
        course2.setCreated_at(LocalDateTime.now());
        courseRepository.save(course2);
    }

    private Payment createPayment(User user, Course course) {
        Payment payment = new Payment();
        payment.setUser(user);
        payment.setCourse(course);
        payment.setAmount(course.getPrice());
        payment.setProvider("STRIPE");
        payment.setTransactionId("tx-" + System.nanoTime());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    // Fetch + data
    @Test
    void findByUserIdWithCourse_shouldReturnPaymentsWithFetchedCourse() {
        createPayment(user, course1);
        createPayment(user, course2);

        List<Payment> result = paymentRepository.findByUserIdWithCourse(user.getId());

        assertThat(result).hasSize(2);

        assertThat(result)
            .extracting(p -> p.getCourse().getTitle())
            .containsExactlyInAnyOrder("Course 1", "Course 2");
    }

    // Empty
    @Test
    void findByUserIdWithCourse_shouldReturnEmpty_whenNoPayments() {
        List<Payment> result = paymentRepository.findByUserIdWithCourse(user.getId());

        assertThat(result).isEmpty();
    }

    // Filter correct user
    @Test
    void findByUserIdWithCourse_shouldReturnOnlyPaymentsOfThatUser() {
        createPayment(user, course1);
        createPayment(anotherUser, course2);

        List<Payment> result = paymentRepository.findByUserIdWithCourse(user.getId());

        assertThat(result).hasSize(1);
        assertThat(result)
            .allMatch(p -> p.getUser().getId().equals(user.getId()));
    }
}