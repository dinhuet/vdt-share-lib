package com.pm.sharedlib.runtime;

public final class SecurityLogRetentionBucketMapper {

    public static final String RETENTION_14_DAYS = "r14";
    public static final String RETENTION_30_DAYS = "r30";
    public static final String RETENTION_90_DAYS = "r90";

    private SecurityLogRetentionBucketMapper() {
    }

    public static String bucket(Integer retentionDays) {
        if (retentionDays == null) {
            return RETENTION_30_DAYS;
        }
        if (retentionDays <= 14) {
            return RETENTION_14_DAYS;
        }
        if (retentionDays <= 30) {
            return RETENTION_30_DAYS;
        }
        return RETENTION_90_DAYS;
    }

    public static int normalizedDays(Integer retentionDays) {
        return retentionDays == null ? 30 : retentionDays;
    }
}
