package com.example.coursehub.enrollment;

import com.example.coursehub.user.dto.UserEnrollmentResponse;

import java.util.List;

public interface EnrollmentService {
    List<UserEnrollmentResponse> getUserEnrollments(Long id);
    void enroll(Long userId, Long courseId);
    void updateProgress(Long id, Integer progress);
    void cancelEnrollment(Long id);
}
