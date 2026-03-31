package com.example.coursehub.auth;

import com.example.coursehub.auth.dto.*;
import com.example.coursehub.common.exception.ErrorCode;
import com.example.coursehub.common.exception.UserError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response){
        String ip = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResult result = authService.login(request.email(), request.password(), ip, userAgent);
        response.addHeader(HttpHeaders.SET_COOKIE,
            cookieUtils.createRefreshTokenCookie(result.refreshToken()).toString());
        return new AuthResponse(result.accessToken(), result.sessionId());
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
        return new AuthResponse(result.accessToken(),  result.sessionId());
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

    @GetMapping("/sessions")
    public List<SessionResponse> getSessions(Authentication authentication) {
        return authService.getSessions(authentication.getName());
    }

    @DeleteMapping("/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSession(@PathVariable String sessionId,
                              Authentication authentication) {
        authService.deleteSession(authentication.getName(), sessionId);
    }

    @DeleteMapping("/sessions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllSessions(
        Authentication authentication,
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        HttpServletResponse response) {
        String accessToken = authHeader != null && authHeader.startsWith("Bearer ")
            ? authHeader.substring(7) : null;

        authService.deleteAllSessions(accessToken, authentication.getName());

        response.addHeader(HttpHeaders.SET_COOKIE,
            cookieUtils.clearRefreshTokenCookie().toString());
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
