package com.pm.be.exception;


public class AppException extends RuntimeException{
    private ErrorCode error;

    public AppException(ErrorCode error) {
        super(error.getMessage());
        this.error = error;
    }

    public ErrorCode getError() {
        return error;
    }

    public void setError(ErrorCode error) {
        this.error = error;
    }
}
