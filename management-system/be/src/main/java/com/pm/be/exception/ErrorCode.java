package com.pm.be.exception;

public enum ErrorCode {
    USER_EXISTED(1001, "Email already exists"),
    USER_NOTFOUND(404, "User not found"),
    UNCATEGORIZED_ERROR(999, "Uncategorized error"),
    EMAIL_INVALID(1003, "Email không hợp lệ "),
    PASSWORD_INVALID(1004, "Password phải có ít nhất 6 ký tự"),
    EMAIL_ALREADY_EXISTS(1005, "Email already exists"),
    PASSWORD_NOT_MATCH(1006, "ConfirmPassword is not match"),
    USERNAME_INVALID(1007, "Username phải có tối thiểu 3 kí tự"),
    INVALID_KEY(1008, "Invalid key"),
    UNAUTHENTICATED(1009, "Unauthenticated"),
    ROLE_NOTFOUND(1010, "Role not found"),

    ;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    private int code;
    private String message;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
