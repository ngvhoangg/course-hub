package com.example.coursehub.auth.jwt;

import com.example.coursehub.auth.CustomUserDetailsService;
import com.example.coursehub.auth.SessionService;
import com.example.coursehub.auth.TokenBlacklistService;
import com.example.coursehub.common.exception.ErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    private final SessionService sessionService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, CustomUserDetailsService userDetailsService, TokenBlacklistService tokenBlacklistService, SessionService sessionService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.sessionService = sessionService;
    }

    private String extractToken(HttpServletRequest request){
        String header = request.getHeader("Authorization");
        if(header != null && header.startsWith("Bearer ")){
            return header.substring(7);
        }
        return null;
    }

    private boolean isPublicEndpoint(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Auth endpoints
        if (uri.startsWith("/api/auth/")) {
            if ("POST".equals(method)) {
                return uri.equals("/api/auth/register")
                    || uri.equals("/api/auth/login")
                    || uri.equals("/api/auth/refresh")
                    || uri.equals("/api/auth/resend-verification");
            }
            if ("GET".equals(method)) {
                return uri.equals("/api/auth/verify");
            }
        }

        // Public GET endpoints
        if ("GET".equals(method)) {
            if (uri.equals("/api/courses")
                || uri.equals("/api/courses/search")
                || uri.equals("/api/categories")
                || uri.equals("/api/courses/semantic")) {
                return true;
            }
            // /api/courses/{id}
            if (uri.matches("^/api/courses/\\d+$")) {
                return true;
            }
            // /api/courses/{id}/reviews
            if (uri.matches("^/api/courses/\\d+/reviews$")) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null) {
            try {
                jwtTokenProvider.validateToken(token);
                String jti = jwtTokenProvider.getJtiFromToken(token);
                if (tokenBlacklistService.isBlacklisted(jti)) {
                    request.setAttribute("errorCode", ErrorCode.TOKEN_BLACKLISTED);
                    throw new AuthenticationCredentialsNotFoundException("Blacklisted");
                }

                String sessionId = jwtTokenProvider.getSessionIdFromToken(token);
                if (!sessionService.sessionExists(sessionId)) {
                    request.setAttribute("errorCode", ErrorCode.SESSION_EXPIRED);
                    throw new AuthenticationCredentialsNotFoundException("Session expired");
                }
                String email = jwtTokenProvider.getEmailFromToken(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (ExpiredJwtException e) {
                request.setAttribute("errorCode", ErrorCode.ACCESS_TOKEN_EXPIRED);
                throw new AuthenticationCredentialsNotFoundException("Token expired");
            } catch (JwtException e) {
                request.setAttribute("errorCode", ErrorCode.INVALID_ACCESS_TOKEN);
                throw new AuthenticationCredentialsNotFoundException("Invalid token");
            }
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return isPublicEndpoint(request);
    }
}
