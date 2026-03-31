package com.example.coursehub.auth;

import com.example.coursehub.auth.dto.AuthResult;
import com.example.coursehub.auth.dto.SessionResponse;

import java.util.List;

public interface AuthService {
    void register(String email, String password, String fullName);
    AuthResult login(String email, String password, String ip, String userAgent);
    void resendVerification(String email);
    void verifyEmail(String token);
    AuthResult refresh(String refreshToken);
    void logout(String accessToken, String refreshToken);
    List<SessionResponse> getSessions(String email);
    void deleteSession(String email, String sessionId);
    void deleteAllSessions(String accessToken, String email);
}
