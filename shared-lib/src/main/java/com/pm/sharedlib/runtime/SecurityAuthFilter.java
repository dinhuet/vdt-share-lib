package com.pm.sharedlib.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.sharedlib.config.VdtShareProperties;
import com.pm.sharedlib.endpoint.EndpointRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class SecurityAuthFilter extends OncePerRequestFilter {

    private static final int FORBIDDEN = 403;
    private static final int TOO_MANY_REQUESTS = 429;
    private static final int PAYLOAD_TOO_LARGE = 413;
    private static final int SERVICE_UNAVAILABLE = 503;
    private static final String ACTIVE = "ACTIVE";
    private static final String HTTP = "HTTP";

    private final EndpointRegistry endpointRegistry;
    private final SecuritySettingsStore settingsStore;
    private final AccessPolicyEvaluator accessPolicyEvaluator;
    private final ClientAuthService clientAuthService;
    private final ClientPermissionChecker clientPermissionChecker;
    private final RateLimiter rateLimiter;
    private final VdtShareProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var endpoint = endpointRegistry.findExposedHttp(request.getMethod(), resolveRequestPath(request));
        if (endpoint.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            var config = settingsStore.getExposedApi(endpoint.get().getEndpointId())
                    .orElseThrow(() -> new RuntimeSecurityException(
                            FORBIDDEN,
                            "EXPOSED_API_CONFIG_MISSING",
                            "Exposed API runtime config was not found"));

            validateExposedApiConfig(config, request);
            request.setAttribute("exposedApiConfig", config);

            var policies = settingsStore.getAccessPolicies(config.getId());
            var decision = accessPolicyEvaluator.evaluate(
                    policies,
                    resolveSourceIp(request),
                    request.getHeader(RuntimeSecurityHeaders.CLIENT_ID));

            if (decision == AccessPolicyDecision.DENY) {
                throw new RuntimeSecurityException(FORBIDDEN, "ACCESS_POLICY_DENIED", "Request was denied by access policy");
            }

            String rateLimitIdentityType = "ip";
            String rateLimitIdentityValue = resolveSourceIp(request);
            if (decision == AccessPolicyDecision.REQUIRE_AUTH) {
                var client = clientAuthService.authenticate(request);
                clientPermissionChecker.checkPermission(client.getClientId(), config.getId());
                rateLimitIdentityType = "client";
                rateLimitIdentityValue = client.getClientId().toString();
            } else if (StringUtils.hasText(request.getHeader(RuntimeSecurityHeaders.CLIENT_ID))) {
                rateLimitIdentityType = "client";
                rateLimitIdentityValue = request.getHeader(RuntimeSecurityHeaders.CLIENT_ID).trim();
            }

            checkRateLimit(config, rateLimitIdentityType, rateLimitIdentityValue);
            long start = System.currentTimeMillis();
            filterChain.doFilter(request, response);
            long elapsed = System.currentTimeMillis() - start;
            if (config.getLatencyThresholdMs() != null && elapsed > config.getLatencyThresholdMs()) {
                log.warn("Exposed API [{}] {} {} latency threshold exceeded: {}ms > {}ms",
                        config.getEndpointId(), config.getMethod(), config.getPath(),
                        elapsed, config.getLatencyThresholdMs());
            }
            if (config.getTimeoutMs() != null && elapsed > config.getTimeoutMs()) {
                log.warn("Exposed API [{}] {} {} timed out: {}ms > {}ms",
                        config.getEndpointId(), config.getMethod(), config.getPath(),
                        elapsed, config.getTimeoutMs());
            }
        } catch (RuntimeSecurityException e) {
            writeError(response, e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (RuntimeException e) {
            if (properties.getRuntime().isFailOpen()) {
                log.warn("Runtime security filter failed, failing open", e);
                filterChain.doFilter(request, response);
                return;
            }
            log.warn("Runtime security filter failed", e);
            writeError(response, SERVICE_UNAVAILABLE, "RUNTIME_SECURITY_UNAVAILABLE", "Runtime security check failed");
        }
    }

    private void validateExposedApiConfig(ExposedApiRuntimeConfig config, HttpServletRequest request) {
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            throw new RuntimeSecurityException(FORBIDDEN, "EXPOSED_API_DISABLED", "Exposed API is disabled");
        }
        if (!ACTIVE.equalsIgnoreCase(config.getSyncStatus())) {
            throw new RuntimeSecurityException(FORBIDDEN, "EXPOSED_API_NOT_ACTIVE", "Exposed API is not active");
        }
        if (!HTTP.equalsIgnoreCase(config.getProtocol())) {
            throw new RuntimeSecurityException(FORBIDDEN, "EXPOSED_API_PROTOCOL_MISMATCH", "Exposed API protocol is not HTTP");
        }
        validateRequestSize(config, request);
    }

    private void validateRequestSize(ExposedApiRuntimeConfig config, HttpServletRequest request) {
        if (config.getMaxRequestKb() == null || config.getMaxRequestKb() <= 0) {
            return;
        }
        long contentLength = request.getContentLengthLong();
        if (contentLength < 0) {
            return;
        }
        long maxBytes = config.getMaxRequestKb() * 1024L;
        if (contentLength > maxBytes) {
            throw new RuntimeSecurityException(PAYLOAD_TOO_LARGE, "REQUEST_TOO_LARGE", "Request body is too large");
        }
    }

    private String resolveRequestPath(HttpServletRequest request) {
        var path = request.getRequestURI();
        var contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            return path.substring(contextPath.length());
        }
        return path;
    }

    private String resolveSourceIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private void checkRateLimit(ExposedApiRuntimeConfig config, String identityType, String identityValue) {
        var result = rateLimiter.check(config, identityType, identityValue);
        if (!result.allowed()) {
            throw new RuntimeSecurityException(
                    TOO_MANY_REQUESTS,
                    "RATE_LIMIT_EXCEEDED",
                    "Rate limit exceeded: " + result.currentRequests() + "/" + result.maxRequests()
                            + " requests in " + result.windowSeconds() + " seconds");
        }
    }

    private void writeError(HttpServletResponse response, int statusCode, String errorCode, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(statusCode);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
                "code", errorCode,
                "message", message
        ));
    }
}
