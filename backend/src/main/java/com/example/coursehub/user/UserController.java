package com.example.coursehub.user;

import com.example.coursehub.payment.dto.PaymentResponse;
import com.example.coursehub.user.dto.CreateUserRequest;
import com.example.coursehub.user.dto.UserEnrollmentResponse;
import com.example.coursehub.user.dto.UserResponse;
import com.example.coursehub.enrollment.EnrollmentService;
import com.example.coursehub.payment.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService  userService;
    private final EnrollmentService enrollmentService;
    private final PaymentService paymentService;

    public UserController(UserService userService, EnrollmentService enrollmentService, PaymentService paymentService) {
        this.userService = userService;
        this.enrollmentService = enrollmentService;
        this.paymentService = paymentService;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public UserResponse getMe(Authentication authentication) {
        return userService.getMe(authentication.getName());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @GetMapping("/{id}/enrollments")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public List<UserEnrollmentResponse> getUserEnrollments(@PathVariable Long id) {
        return enrollmentService.getUserEnrollments(id);
    }

    @GetMapping("/{id}/payments")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public List<PaymentResponse> getUserPayments(@PathVariable Long id) {
        return paymentService.getUserPayments(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public void createUser(@Valid @RequestBody CreateUserRequest request) {
        userService.createUser(request.email(), request.password(), request.fullName(), request.role());
    }
}
