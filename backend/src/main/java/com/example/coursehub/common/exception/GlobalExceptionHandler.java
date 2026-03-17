package com.example.coursehub.common.exception;

import com.example.coursehub.common.dto.ApiErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // Handle business/user errors
    @ExceptionHandler(UserError.class)
    public ResponseEntity<ApiErrorResponse>  handleUserError(UserError userError) {
        HttpStatus status = userError.getErrorCode().getHttpStatus();

        ApiErrorResponse response = new ApiErrorResponse(
            userError.getMessage(),
            userError.getErrorCode().name(),
            status.value(),
            System.currentTimeMillis()
        );

        return ResponseEntity.status(status).body(response);
    }

    // Handle system errors (500)
    @ExceptionHandler(SystemError.class)
    public ResponseEntity<ApiErrorResponse> handleSystemError(SystemError systemError) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        ApiErrorResponse response = new ApiErrorResponse(
            ErrorCode.INTERNAL_ERROR.getMessage(),
            systemError.getErrorCode().name(),
            status.value(),
            System.currentTimeMillis()
        );

        return ResponseEntity.status(status).body(response);
    }

    // Handle @Valid validation errors (400)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationError(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));

        HttpStatus status = HttpStatus.BAD_REQUEST;

        ApiErrorResponse response = new ApiErrorResponse(
            message,
            ErrorCode.INVALID_INPUT.name(),
            status.value(),
            System.currentTimeMillis()
        );

        return ResponseEntity.status(status).body(response);
    }

    // Handle constraint violations (e.g. @RequestParam)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations()
            .stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .collect(Collectors.joining(", "));

        HttpStatus status = HttpStatus.BAD_REQUEST;

        ApiErrorResponse response = new ApiErrorResponse(
            message,
            ErrorCode.INVALID_INPUT.name(),
            status.value(),
            System.currentTimeMillis()
        );

        return ResponseEntity.status(status).body(response);
    }

    // Resource not found
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        HttpStatus status = HttpStatus.NOT_FOUND;

        ApiErrorResponse response = new ApiErrorResponse(
            ErrorCode.RESOURCE_NOT_FOUND.getMessage(),
            ErrorCode.RESOURCE_NOT_FOUND.name(),
            status.value(),
            System.currentTimeMillis()
        );

        return ResponseEntity.status(status).body(response);
    }

    // Authorization
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        HttpStatus status = HttpStatus.FORBIDDEN;

        ApiErrorResponse response = new ApiErrorResponse(
            ErrorCode.ACCESS_DENIED.getMessage(),
            ErrorCode.ACCESS_DENIED.name(),
            status.value(),
            System.currentTimeMillis()
        );

        return ResponseEntity.status(status).body(response);
    }

    // Authentication
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;

        ApiErrorResponse response = new ApiErrorResponse(
            ErrorCode.UNAUTHORIZED.getMessage(),
            ErrorCode.UNAUTHORIZED.name(),
            status.value(),
            System.currentTimeMillis()
        );

        return ResponseEntity.status(status).body(response);
    }

    // Handle missing or malformed request body (400)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ApiErrorResponse response = new ApiErrorResponse(
            "Request body is missing or malformed",
            ErrorCode.INVALID_INPUT.name(),
            status.value(),
            System.currentTimeMillis()
        );

        return ResponseEntity.status(status).body(response);
    }

    // Fallback handler (any unexpected error)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        ApiErrorResponse response = new ApiErrorResponse(
            "Unexpected error occurred",
            ErrorCode.INTERNAL_ERROR.name(),
            status.value(),
            System.currentTimeMillis()
        );

        ex.printStackTrace();

        return ResponseEntity.status(status).body(response);
    }
}
