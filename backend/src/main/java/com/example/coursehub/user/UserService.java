package com.example.coursehub.user;

import com.example.coursehub.user.dto.UserResponse;

public interface UserService {
    UserResponse getUserById(Long id);
    UserResponse getMe(String email);
    void createUser(String email, String password, String fullName, Role role);
}
