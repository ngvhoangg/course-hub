package com.example.coursehub.enrollment;

import com.example.coursehub.user.dto.UserEnrollmentResponse;
import org.springframework.stereotype.Component;

@Component
public class EnrollmentMapper {
    public UserEnrollmentResponse toUserEnrollmentResponse(Enrollment enrollment) {
        return new UserEnrollmentResponse(
            enrollment.getCourse().getId(),
            enrollment.getCourse().getTitle(),
            enrollment.getProgress(),
            enrollment.getStatus()
        );
    }
}
