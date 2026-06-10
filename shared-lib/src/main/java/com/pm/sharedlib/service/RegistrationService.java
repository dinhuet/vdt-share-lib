package com.pm.sharedlib.service;

import com.pm.sharedlib.endpoint.EndpointDefinition;
import com.pm.sharedlib.endpoint.EndpointRegistry;
import com.pm.sharedlib.kafka.RegistrationEventProducer;
import com.pm.sharedlib.model.ServiceRegistrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    private final EndpointRegistry endpointRegistry;
    private final RegistrationEventProducer producer;
    private final Environment environment;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        var serviceName = environment.getProperty("spring.application.name");
        if (!StringUtils.hasText(serviceName)) {
            throw new IllegalStateException("spring.application.name must be configured when vdt.share.enabled=true");
        }

        endpointRegistry.initialize(serviceName);
        var serviceUrl = buildServiceUrl();

        var event = ServiceRegistrationEvent.builder()
                .eventType("SERVICE_STARTED")
                .serviceName(serviceName)
                .serviceUrl(serviceUrl)
                .exposedApis(endpointRegistry.getExposedApis().stream().map(this::toApiInfo).toList())
                .clientApis(endpointRegistry.getClientApis().stream().map(this::toApiInfo).toList())
                .build();

        producer.send(event);
    }

    private ServiceRegistrationEvent.ApiInfo toApiInfo(EndpointDefinition endpoint) {
        return ServiceRegistrationEvent.ApiInfo.builder()
                .endpointId(endpoint.getEndpointId())
                .endpointKey(endpoint.getEndpointKey())
                .name(endpoint.getName())
                .path(endpoint.getPath())
                .destinationUrl(endpoint.getDestinationUrl())
                .topic(endpoint.getTopic())
                .method(endpoint.getMethod())
                .protocol(endpoint.getProtocol())
                .build();
    }

    private String buildServiceUrl() {
        var port = environment.getProperty("server.port", Integer.class, 8080);
        var sslEnabled = environment.getProperty("server.ssl.enabled", Boolean.class, false);
        var protocol = sslEnabled ? "https" : "http";
        var contextPath = environment.getProperty("server.servlet.context-path", "");

        return protocol + "://localhost:" + port + contextPath;
    }
}
