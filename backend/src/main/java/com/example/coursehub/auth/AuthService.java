package com.example.coursehub.auth;

import com.example.coursehub.auth.dto.AuthResult;

public interface AuthService {
    void register(String email, String password, String fullName);
    AuthResult login(String email, String password);
    void resendVerification(String email);
    void verifyEmail(String token);
    AuthResult refresh(String refreshToken);
    void logout(String accessToken, String refreshToken);
}
