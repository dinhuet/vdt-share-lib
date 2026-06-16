package com.pm.sharedlib.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.sharedlib.config.VdtShareProperties;
import com.pm.sharedlib.endpoint.EndpointDefinition;
import com.pm.sharedlib.endpoint.EndpointRegistry;
import com.pm.sharedlib.endpoint.EndpointType;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityAuthFilterTest {

    private static final UUID ENDPOINT_ID = UUID.randomUUID();
    private static final UUID CONFIG_ID = UUID.randomUUID();

    @Mock EndpointRegistry endpointRegistry;
    @Mock SecuritySettingsStore settingsStore;
    @Mock AccessPolicyEvaluator accessPolicyEvaluator;
    @Mock ClientAuthService clientAuthService;
    @Mock ClientPermissionChecker clientPermissionChecker;
    @Mock RateLimiter rateLimiter;
    @Mock SecurityAuditLogger auditLogger;
    @Mock FilterChain filterChain;

    SecurityAuthFilter filter;
    EndpointDefinition endpoint;
    ExposedApiRuntimeConfig config;

    @BeforeEach
    void setUp() {
        filter = new SecurityAuthFilter(
                endpointRegistry,
                settingsStore,
                accessPolicyEvaluator,
                clientAuthService,
                clientPermissionChecker,
                rateLimiter,
                new VdtShareProperties(),
                new ObjectMapper(),
                auditLogger);
        endpoint = EndpointDefinition.builder()
                .endpointId(ENDPOINT_ID)
                .type(EndpointType.EXPOSED)
                .protocol("HTTP")
                .name("orders")
                .method("GET")
                .path("/orders")
                .build();
        config = ExposedApiRuntimeConfig.builder()
                .id(CONFIG_ID)
                .endpointId(ENDPOINT_ID)
                .apiName("orders")
                .method("GET")
                .path("/orders")
                .protocol("HTTP")
                .enabled(true)
                .syncStatus("ACTIVE")
                .build();
    }

    @Test
    void doFilter_shouldNotAuditWhenEndpointDoesNotMatch() throws Exception {
        var request = request();
        var response = new MockHttpServletResponse();
        when(endpointRegistry.findExposedHttp("GET", "/orders")).thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(auditLogger, never()).log(any());
    }

    @Test
    void doFilter_shouldAuditSuccess() throws Exception {
        setupConfig(AccessPolicyDecision.ALLOW_TRUSTED);
        when(rateLimiter.check(config, "ip", "127.0.0.1")).thenReturn(new RateLimitResult(true, 1, 10, 60));
        var request = request();

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        verify(auditLogger).log(org.mockito.ArgumentMatchers.argThat(event ->
                "INBOUND_HTTP".equals(event.getFlowType()) && "SUCCESS".equals(event.getStatus())));
    }

    @Test
    void doFilter_shouldAuditAccessPolicyDeny() throws Exception {
        setupConfig(AccessPolicyDecision.DENY);

        filter.doFilter(request(), new MockHttpServletResponse(), filterChain);

        verify(auditLogger).log(org.mockito.ArgumentMatchers.argThat(event ->
                "INBOUND_HTTP".equals(event.getFlowType()) && "ACCESS_POLICY_DENIED".equals(event.getErrorCode())));
        verify(filterChain, never()).doFilter(any(), any());
    }

    private void setupConfig(AccessPolicyDecision decision) {
        when(endpointRegistry.findExposedHttp("GET", "/orders")).thenReturn(Optional.of(endpoint));
        when(settingsStore.getExposedApi(ENDPOINT_ID)).thenReturn(Optional.of(config));
        when(settingsStore.getAccessPolicies(CONFIG_ID)).thenReturn(List.of());
        when(accessPolicyEvaluator.evaluate(any(), eq("127.0.0.1"), any())).thenReturn(decision);
    }

    private MockHttpServletRequest request() {
        var request = new MockHttpServletRequest("GET", "/orders");
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}
