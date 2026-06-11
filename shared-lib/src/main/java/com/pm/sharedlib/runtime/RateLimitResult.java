package com.pm.sharedlib.runtime;

public record RateLimitResult(boolean allowed, long currentRequests, int maxRequests, long windowSeconds) {
}
