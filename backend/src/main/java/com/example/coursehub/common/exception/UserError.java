package com.example.coursehub.common.exception;

public class UserError extends AppException {
    public UserError(ErrorCode errorCode) {
        super(errorCode, null);
    }

    public UserError(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
