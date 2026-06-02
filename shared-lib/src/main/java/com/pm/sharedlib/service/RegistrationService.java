package com.pm.sharedlib.service;

import com.pm.sharedlib.annotation.ClientCall;
import com.pm.sharedlib.annotation.SharedApi;
import com.pm.sharedlib.kafka.RegistrationEventProducer;
import com.pm.sharedlib.model.ServiceRegistrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.ArrayList;

@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    private final ListableBeanFactory beanFactory;
    private final RegistrationEventProducer producer;
    private final Environment environment;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        var exposedApis = new ArrayList<ServiceRegistrationEvent.ApiInfo>();
        var clientApis = new ArrayList<ServiceRegistrationEvent.ApiInfo>();

        for (var beanName : beanFactory.getBeanDefinitionNames()) {
            var bean = beanFactory.getBean(beanName);
            for (var method : bean.getClass().getMethods()) {
                var sharedApi = method.getAnnotation(SharedApi.class);
                if (sharedApi != null) {
                    exposedApis.add(ServiceRegistrationEvent.ApiInfo.builder()
                            .name(sharedApi.name())
                            .path(sharedApi.path())
                            .method(sharedApi.method())
                            .protocol(sharedApi.protocol())
                            .build());
                }
                var clientCall = method.getAnnotation(ClientCall.class);
                if (clientCall != null) {
                    clientApis.add(ServiceRegistrationEvent.ApiInfo.builder()
                            .name(clientCall.name())
                            .destinationUrl(clientCall.destinationUrl())
                            .method(clientCall.method())
                            .protocol(clientCall.protocol())
                            .build());
                }
            }
        }

        var serviceName = environment.getProperty("spring.application.name");
        if (!StringUtils.hasText(serviceName)) {
            throw new IllegalStateException("spring.application.name must be configured when vdt.share.enabled=true");
        }
        var serviceUrl = buildServiceUrl();

        var event = ServiceRegistrationEvent.builder()
                .eventType("SERVICE_STARTED")
                .serviceName(serviceName)
                .serviceUrl(serviceUrl)
                .exposedApis(exposedApis)
                .clientApis(clientApis)
                .build();

        producer.send(event);
    }

    private String buildServiceUrl() {
        var port = environment.getProperty("server.port", Integer.class, 8080);
        var sslEnabled = environment.getProperty("server.ssl.enabled", Boolean.class, false);
        var protocol = sslEnabled ? "https" : "http";
        var contextPath = environment.getProperty("server.servlet.context-path", "");

        return protocol + "://localhost:" + port + contextPath;
    }
}
