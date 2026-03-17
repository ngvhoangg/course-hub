package com.example.coursehub.user.dto;

import com.example.coursehub.user.UserStatus;

public record UserResponse(
    Long id,
    String email,
    String fullName,
    UserStatus status
) {}
