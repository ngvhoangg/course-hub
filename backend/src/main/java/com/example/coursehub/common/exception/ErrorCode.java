package com.example.coursehub.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // 404
    USER_NOT_FOUND("User not found", HttpStatus.NOT_FOUND),
    COURSE_NOT_FOUND("Course not found", HttpStatus.NOT_FOUND),
    INSTRUCTOR_NOT_FOUND("Instructor not found", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND("One or more categories not found", HttpStatus.NOT_FOUND),
    ENROLLMENT_NOT_FOUND("Enrollment not found", HttpStatus.NOT_FOUND),
    LESSON_NOT_FOUND("Lesson not found", HttpStatus.NOT_FOUND),
    RESOURCE_NOT_FOUND("Resource not found", HttpStatus.NOT_FOUND),
    VERIFICATION_TOKEN_NOT_FOUND("Verification token not found", HttpStatus.NOT_FOUND),
    REFRESH_TOKEN_NOT_FOUND("Refresh token not found", HttpStatus.NOT_FOUND),
    SESSION_NOT_FOUND("Session not found", HttpStatus.NOT_FOUND),

    //403
    ACCESS_DENIED("Access denied", HttpStatus.FORBIDDEN),
    EMAIL_NOT_VERIFIED("Please verify your email before logging in", HttpStatus.FORBIDDEN),
    ACCOUNT_DISABLED("Your account has been disabled", HttpStatus.FORBIDDEN),

    //401
    UNAUTHORIZED("Authentication required", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED("Refresh token has expired", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_REVOKED("Refresh token has been revoked", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_REUSED("Refresh token reuse detected, please login again", HttpStatus.UNAUTHORIZED),
    SESSION_EXPIRED("Session has expired", HttpStatus.UNAUTHORIZED),
    TOKEN_BLACKLISTED("Token has been blacklisted", HttpStatus.UNAUTHORIZED),
    ACCESS_TOKEN_EXPIRED("Access token has expired", HttpStatus.UNAUTHORIZED),
    INVALID_ACCESS_TOKEN("Invalid access token", HttpStatus.UNAUTHORIZED),

    // 400
    INVALID_INPUT("Invalid input data", HttpStatus.BAD_REQUEST),
    EMPTY_UPDATE_REQUEST("At least one field must be provided for update", HttpStatus.BAD_REQUEST),
    INVALID_ROLE_ASSIGNMENT("Cannot assign this role", HttpStatus.BAD_REQUEST),
    VERIFICATION_TOKEN_EXPIRED("Verification token has expired", HttpStatus.BAD_REQUEST),
    VERIFICATION_TOKEN_ALREADY_USED("Verification token has already been used", HttpStatus.BAD_REQUEST),
    ALREADY_VERIFIED("Email is already verified", HttpStatus.BAD_REQUEST),

    // 409
    ALREADY_ENROLLED("You are already enrolled in this course", HttpStatus.CONFLICT),
    REVIEW_ALREADY_EXISTS("Review already exists", HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS("Email is already in use", HttpStatus.CONFLICT),

    // 409
    RATE_LIMIT_EXCEEDED("Too many requests, please try again later", HttpStatus.TOO_MANY_REQUESTS),

    // 500
    EMAIL_SEND_FAILED("Failed to send email", HttpStatus.INTERNAL_SERVER_ERROR),
    INTERNAL_ERROR("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String message, HttpStatus httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
