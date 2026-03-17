package com.example.coursehub.enrollment;

import com.example.coursehub.enrollment.dto.EnrollmentProgressRequest;
import com.example.coursehub.enrollment.dto.EnrollmentRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {
    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    public void enroll(@Valid @RequestBody EnrollmentRequest request){
        enrollmentService.enroll(request.userId(), request.courseId());
    }

    @PatchMapping("/{id}/progress")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('STUDENT')")
    public void updateProgress(
        @PathVariable Long id,
        @Valid @RequestBody EnrollmentProgressRequest request)
    {
        enrollmentService.updateProgress(id, request.progress());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('STUDENT')")
    public void cancelEnrollment(@PathVariable Long id) {
        enrollmentService.cancelEnrollment(id);
    }
}
