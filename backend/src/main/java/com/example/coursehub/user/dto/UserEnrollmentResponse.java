package com.example.coursehub.user.dto;

import com.example.coursehub.enrollment.EnrollmentStatus;

public record UserEnrollmentResponse(
   Long courseId,
   String courseTitle,
   Integer progress,
   EnrollmentStatus status
) {}
