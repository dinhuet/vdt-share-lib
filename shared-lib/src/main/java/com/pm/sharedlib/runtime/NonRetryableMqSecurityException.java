package com.pm.sharedlib.runtime;

public class NonRetryableMqSecurityException extends RuntimeSecurityException {

    public NonRetryableMqSecurityException(int statusCode, String errorCode, String message) {
        super(statusCode, errorCode, message);
    }
}
