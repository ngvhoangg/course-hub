package com.example.coursehub.common.ratelimit;

import com.example.coursehub.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock private RateLimitService rateLimitService;
    @Mock private ObjectMapper objectMapper;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private RateLimitFilter rateLimitFilter;

    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        responseWriter = new StringWriter();
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        lenient().when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        rateLimitFilter = new RateLimitFilter(rateLimitService, objectMapper);
        ReflectionTestUtils.setField(rateLimitFilter, "enabled", true);
    }

    // login
    @Test
    void doFilterInternal_shouldAllow_whenLoginWithinLimit() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(rateLimitService.tryConsume("login:127.0.0.1", 5, Duration.ofMinutes(1))).thenReturn(true);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldBlock_whenLoginExceedsLimit() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(rateLimitService.tryConsume("login:127.0.0.1", 5, Duration.ofMinutes(1))).thenReturn(false);
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        verify(filterChain, never()).doFilter(any(), any());
    }

    // register
    @Test
    void doFilterInternal_shouldAllow_whenRegisterWithinLimit() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/register");
        when(request.getMethod()).thenReturn("POST");
        when(rateLimitService.tryConsume("register:127.0.0.1", 3, Duration.ofMinutes(1))).thenReturn(true);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldBlock_whenRegisterExceedsLimit() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/register");
        when(request.getMethod()).thenReturn("POST");
        when(rateLimitService.tryConsume("register:127.0.0.1", 3, Duration.ofMinutes(1))).thenReturn(false);
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
        verify(filterChain, never()).doFilter(any(), any());
    }

    // verify
    @Test
    void doFilterInternal_shouldAllow_whenVerifyWithinLimit() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/verify");
        when(request.getMethod()).thenReturn("GET");
        when(rateLimitService.tryConsume("verify:127.0.0.1", 10, Duration.ofMinutes(1))).thenReturn(true);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldBlock_whenVerifyExceedsLimit() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/verify");
        when(request.getMethod()).thenReturn("GET");
        when(rateLimitService.tryConsume("verify:127.0.0.1", 10, Duration.ofMinutes(1))).thenReturn(false);
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
        verify(filterChain, never()).doFilter(any(), any());
    }

    // resend-verification
    @Test
    void doFilterInternal_shouldUseEmailKey_whenEmailProvided() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/resend-verification");
        when(request.getMethod()).thenReturn("POST");
        when(request.getParameter("email")).thenReturn("test@example.com");
        when(rateLimitService.tryConsume("resend:test@example.com", 3, Duration.ofHours(1))).thenReturn(true);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldUseIpKey_whenEmailNotProvided() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/resend-verification");
        when(request.getMethod()).thenReturn("POST");
        when(request.getParameter("email")).thenReturn(null);
        when(rateLimitService.tryConsume("resend:127.0.0.1", 3, Duration.ofHours(1))).thenReturn(true);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    // unrelated path
    @Test
    void doFilterInternal_shouldAllow_whenPathNotRateLimited() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/courses");

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimitService, never()).tryConsume(any(), anyInt(), any());
    }

    // X-Forwarded-For
    @Test
    void doFilterInternal_shouldUseForwardedIp_whenXForwardedForPresent() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");
        when(rateLimitService.tryConsume("login:192.168.1.1", 5, Duration.ofMinutes(1))).thenReturn(true);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(rateLimitService).tryConsume("login:192.168.1.1", 5, Duration.ofMinutes(1));
        verify(filterChain).doFilter(request, response);
    }
}