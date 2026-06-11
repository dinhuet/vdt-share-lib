package com.pm.sharedlib.runtime;

public final class RuntimeSecurityErrorCodes {

    public static final String AUTH_HEADER_MISSING = "AUTH_HEADER_MISSING";
    public static final String AUTH_CLIENT_ID_INVALID = "AUTH_CLIENT_ID_INVALID";
    public static final String AUTH_CREDENTIAL_NOT_FOUND = "AUTH_CREDENTIAL_NOT_FOUND";
    public static final String AUTH_CREDENTIAL_INACTIVE = "AUTH_CREDENTIAL_INACTIVE";
    public static final String AUTH_CREDENTIAL_EXPIRED = "AUTH_CREDENTIAL_EXPIRED";
    public static final String AUTH_API_KEY_INVALID = "AUTH_API_KEY_INVALID";
    public static final String AUTH_CLIENT_MISMATCH = "AUTH_CLIENT_MISMATCH";
    public static final String PERMISSION_DENIED = "PERMISSION_DENIED";
    public static final String RESPONSE_TOO_LARGE = "RESPONSE_TOO_LARGE";

    private RuntimeSecurityErrorCodes() {
    }
}
