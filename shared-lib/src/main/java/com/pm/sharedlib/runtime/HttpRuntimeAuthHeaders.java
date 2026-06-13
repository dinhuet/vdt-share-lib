package com.pm.sharedlib.runtime;

import jakarta.servlet.http.HttpServletRequest;

public class HttpRuntimeAuthHeaders implements RuntimeAuthHeaders {

    private final HttpServletRequest request;

    public HttpRuntimeAuthHeaders(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public String get(String name) {
        return request.getHeader(name);
    }
}
