package com.example.coursehub.user.dto;

import com.example.coursehub.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password is required")
    String password,

    @NotBlank(message = "Full name is required")
    String fullName,

    @NotNull(message = "Role is required")
    Role role
) {}