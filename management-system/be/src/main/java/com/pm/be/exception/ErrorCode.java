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
    MICROSERVICE_NOTFOUND(1011, "Micro service not found"),
    API_DEFAULT_CONFIG_NOTFOUND(1012, "API default config not found"),
    API_DEFAULT_CONFIG_INVALID(1013, "API default config invalid"),
    EXPOSED_API_NOTFOUND(1014, "Exposed API not found"),
    EXPOSED_API_INVALID_SETTING(1015, "Exposed API setting invalid"),
    CLIENT_API_NOTFOUND(1016, "Client API not found"),
    CLIENT_API_INVALID(1017, "Client API invalid"),
    CLIENT_API_EXISTED(1018, "Client API already exists"),
    EXPOSED_API_DELETE_NOT_ALLOWED(1019, "Only stale exposed APIs can be deleted"),
    CLIENT_API_DELETE_NOT_ALLOWED(1020, "Only stale client APIs can be deleted"),
    CLIENT_NOTFOUND(1021, "Client not found"),
    CLIENT_EXISTED(1022, "Client already exists"),
    CLIENT_INVALID(1023, "Client invalid"),
    CLIENT_STATUS_INVALID(1024, "Client status invalid"),
    CLIENT_DELETE_NOT_ALLOWED(1025, "Only revoked clients can be deleted"),
    CLIENT_CREDENTIAL_NOTFOUND(1026, "Client credential not found"),
    CLIENT_CREDENTIAL_EXISTED(1027, "Client credential already exists"),
    CLIENT_CREDENTIAL_INVALID(1028, "Client credential invalid"),
    CLIENT_CREDENTIAL_REVOKE_NOT_ALLOWED(1029, "Only active client credentials can be revoked"),
    CLIENT_CREDENTIAL_SECRET_INVALID(1030, "Client credential secret invalid"),
    ACCESS_POLICY_NOTFOUND(1031, "Access policy not found"),
    ACCESS_POLICY_INVALID(1032, "Access policy invalid"),
    ACCESS_POLICY_EXISTED(1033, "Access policy already exists"),
    CLIENT_PERMISSION_NOTFOUND(1034, "Client permission not found"),
    CLIENT_PERMISSION_INVALID(1035, "Client permission invalid"),
    CLIENT_PERMISSION_EXISTED(1036, "Client permission already exists"),
    ANOMALY_RULE_NOTFOUND(1037, "Anomaly rule not found"),
    ANOMALY_RULE_INVALID(1038, "Anomaly rule invalid"),
    ANOMALY_RULE_EXISTED(1039, "Anomaly rule already exists"),

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
