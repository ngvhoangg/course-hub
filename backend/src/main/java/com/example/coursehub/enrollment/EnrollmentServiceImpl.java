package com.example.coursehub.enrollment;

import com.example.coursehub.course.Course;
import com.example.coursehub.user.User;
import com.example.coursehub.user.dto.UserEnrollmentResponse;
import com.example.coursehub.common.exception.ErrorCode;
import com.example.coursehub.common.exception.UserError;
import com.example.coursehub.course.CourseRepository;
import com.example.coursehub.user.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EnrollmentServiceImpl implements EnrollmentService {
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentMapper enrollmentMapper;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    public EnrollmentServiceImpl(EnrollmentRepository enrollmentRepository, EnrollmentMapper enrollmentMapper, UserRepository userRepository, CourseRepository courseRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentMapper = enrollmentMapper;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    public List<UserEnrollmentResponse> getUserEnrollments(Long id) {
        return enrollmentRepository.findByUserIdWithCourse(id)
            .stream()
            .map(enrollmentMapper::toUserEnrollmentResponse)
            .toList();
    }

    @Override
    @Transactional
    public void enroll(Long userId, Long courseId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserError(ErrorCode.USER_NOT_FOUND));

        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new UserError(ErrorCode.COURSE_NOT_FOUND));

        if(enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new UserError(ErrorCode.ALREADY_ENROLLED);
        }

        Enrollment  enrollment = new Enrollment();
        enrollment.setUser(user);
        enrollment.setCourse(course);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setProgress(0);
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);
    }

    @Override
    @Transactional
    public void updateProgress(Long id, Integer progress) {
        Enrollment enrollment = enrollmentRepository.findById(id)
            .orElseThrow(() -> new UserError(ErrorCode.ENROLLMENT_NOT_FOUND));

        enrollment.setProgress(progress);
        if(progress.equals(100)) {
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
        }
    }

    @Override
    @Transactional
    public void cancelEnrollment(Long id) {
        Enrollment enrollment = enrollmentRepository.findById(id)
            .orElseThrow(() -> new UserError(ErrorCode.ENROLLMENT_NOT_FOUND));

        enrollment.setStatus(EnrollmentStatus.CANCELLED);
    }
}
