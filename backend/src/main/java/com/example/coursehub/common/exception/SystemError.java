package com.example.coursehub.common.exception;

public class SystemError extends AppException {
    public SystemError(ErrorCode errorCode) {
        super(errorCode, null);
    }

    public SystemError(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
