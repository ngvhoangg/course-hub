package com.example.coursehub.auth;

import com.example.coursehub.auth.dto.*;
import com.example.coursehub.common.exception.ErrorCode;
import com.example.coursehub.common.exception.UserError;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final CookieUtils cookieUtils;

    public AuthController(AuthService authService, CookieUtils cookieUtils) {
        this.authService = authService;
        this.cookieUtils = cookieUtils;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest request){
        authService.register(request.email(), request.password(), request.fullName());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response){
        AuthResult result = authService.login(request.email(), request.password());
        response.addHeader(HttpHeaders.SET_COOKIE,
            cookieUtils.createRefreshTokenCookie(result.refreshToken()).toString());
        return new AuthResponse(result.accessToken());
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resendVerification(@RequestBody ResendVerificationRequest request) {
        authService.resendVerification(request.email());
    }

    @GetMapping("/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@CookieValue(name = "refreshToken", required = false) String refreshToken,
                                HttpServletResponse response) {
        if (refreshToken == null) {
            throw new UserError(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        AuthResult result = authService.refresh(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE,
            cookieUtils.createRefreshTokenCookie(result.refreshToken()).toString());
        return new AuthResponse(result.accessToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
        @CookieValue(name = "refreshToken", required = false) String refreshToken,
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        HttpServletResponse response) {
        String accessToken = authHeader != null && authHeader.startsWith("Bearer ")
            ? authHeader.substring(7) : null;

        authService.logout(accessToken, refreshToken);

        response.addHeader(HttpHeaders.SET_COOKIE,
            cookieUtils.clearRefreshTokenCookie().toString());
    }
}
