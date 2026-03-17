package com.example.coursehub.common.ratelimit;

import com.example.coursehub.common.dto.ApiErrorResponse;
import com.example.coursehub.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimitService rateLimitService, ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
    }

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String ip = getClientIp(request);
        boolean allowed = true;

        if (path.equals("/api/auth/login") && request.getMethod().equals("POST")) {
            allowed = rateLimitService.tryConsume("login:" + ip, 5, Duration.ofMinutes(1));
        } else if (path.equals("/api/auth/register") && request.getMethod().equals("POST")) {
            allowed = rateLimitService.tryConsume("register:" + ip, 3, Duration.ofMinutes(1));
        } else if (path.equals("/api/auth/verify") && request.getMethod().equals("GET")) {
            allowed = rateLimitService.tryConsume("verify:" + ip, 10, Duration.ofMinutes(1));
        } else if (path.equals("/api/auth/resend-verification") && request.getMethod().equals("POST")) {
            String email = request.getParameter("email");
            String key = email != null ? "resend:" + email : "resend:" + ip;
            allowed = rateLimitService.tryConsume(key, 3, Duration.ofHours(1));
        }

        if (!allowed) {
            HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;
            ApiErrorResponse errorResponse = new ApiErrorResponse(
                ErrorCode.RATE_LIMIT_EXCEEDED.getMessage(),
                ErrorCode.RATE_LIMIT_EXCEEDED.name(),
                status.value(),
                System.currentTimeMillis()
            );
            response.setStatus(status.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
