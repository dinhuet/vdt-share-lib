package com.pm.sharedlib.runtime;

public class OutboundException extends RuntimeException {

    private final OutboundErrorCode errorCode;

    public OutboundException(OutboundErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public OutboundException(OutboundErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public OutboundErrorCode getErrorCode() {
        return errorCode;
    }
}
