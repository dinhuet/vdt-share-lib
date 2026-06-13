package com.pm.sharedlib.runtime;

public class RetryableMqSecurityException extends RuntimeSecurityException {

    public RetryableMqSecurityException(int statusCode, String errorCode, String message) {
        super(statusCode, errorCode, message);
    }
}
