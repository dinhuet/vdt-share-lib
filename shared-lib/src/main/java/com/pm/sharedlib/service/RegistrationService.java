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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

        var serviceName = environment.getProperty("spring.application.name", "unknown");
        var serviceUrl = buildServiceUrl();

        var keyService = environment.getProperty("vdt.share.key", "unknown");
        saveKeyToFile(keyService);

        var event = ServiceRegistrationEvent.builder()
                .eventType("SERVICE_STARTED")
                .serviceName(serviceName)
                .serviceUrl(serviceUrl)
                .keyService(keyService)
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

    private void saveKeyToFile(String key) {
        try {
            Path path = Path.of("shared-lib/service-key");

            if (!Files.exists(path)) {
                Files.createFile(path);
            }

            String existingContent = Files.readString(path);

            if (existingContent != null && existingContent.contains(key)) {
                log.info("Key already exists in file, skip writing");
                return;
            }

            String newContent = (existingContent == null ? "" : existingContent + System.lineSeparator()) + key;

            Files.writeString(path, newContent);

            log.info("Saved service key to file");

        } catch (IOException e) {
            log.warn("Failed to save service key to file", e);
        }
    }
}
