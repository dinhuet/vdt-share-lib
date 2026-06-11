package com.pm.sharedlib.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pm.sharedlib.endpoint.EndpointManifestStore;
import com.pm.sharedlib.endpoint.EndpointRegistry;
import com.pm.sharedlib.endpoint.EndpointScanner;
import com.pm.sharedlib.kafka.RegistrationEventProducer;
import com.pm.sharedlib.runtime.AccessPolicyEvaluator;
import com.pm.sharedlib.runtime.ClientAuthService;
import com.pm.sharedlib.runtime.ClientPermissionChecker;
import com.pm.sharedlib.runtime.SecurityAuthFilter;
import com.pm.sharedlib.runtime.SecuritySettingsStore;
import com.pm.sharedlib.service.RegistrationService;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.nio.file.Path;

@AutoConfiguration
@ConditionalOnProperty(prefix = "vdt.share", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(VdtShareProperties.class)
public class VdtShareAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Bean
    public RegistrationEventProducer registrationEventProducer(
            KafkaTemplate<String, String> kafka,
            ObjectMapper objectMapper) {
        return new RegistrationEventProducer(kafka, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public EndpointManifestStore endpointManifestStore(
            ObjectMapper objectMapper,
            VdtShareProperties properties,
            Environment environment) {
        var path = environment.resolvePlaceholders(properties.getEndpointManifestPath());
        return new EndpointManifestStore(objectMapper, Path.of(path));
    }

    @Bean
    @ConditionalOnMissingBean
    public EndpointScanner endpointScanner(
            ListableBeanFactory beanFactory,
            ObjectProvider<RequestMappingHandlerMapping> requestMappingHandlerMapping) {
        return new EndpointScanner(beanFactory, requestMappingHandlerMapping);
    }

    @Bean
    @ConditionalOnMissingBean
    public EndpointRegistry endpointRegistry(
            EndpointScanner endpointScanner,
            EndpointManifestStore endpointManifestStore) {
        return new EndpointRegistry(endpointScanner, endpointManifestStore);
    }

    @Bean
    @ConditionalOnMissingBean
    public SecuritySettingsStore securitySettingsStore(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            VdtShareProperties properties) {
        return new SecuritySettingsStore(redisTemplate, objectMapper, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessPolicyEvaluator accessPolicyEvaluator() {
        return new AccessPolicyEvaluator();
    }

    @Bean
    @ConditionalOnMissingBean
    public ClientAuthService clientAuthService(SecuritySettingsStore securitySettingsStore) {
        return new ClientAuthService(securitySettingsStore);
    }

    @Bean
    @ConditionalOnMissingBean
    public ClientPermissionChecker clientPermissionChecker(SecuritySettingsStore securitySettingsStore) {
        return new ClientPermissionChecker(securitySettingsStore);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "vdt.share.runtime", name = "http-filter-enabled", havingValue = "true", matchIfMissing = true)
    public SecurityAuthFilter securityAuthFilter(
            EndpointRegistry endpointRegistry,
            SecuritySettingsStore securitySettingsStore,
            AccessPolicyEvaluator accessPolicyEvaluator,
            ClientAuthService clientAuthService,
            ClientPermissionChecker clientPermissionChecker,
            VdtShareProperties properties,
            ObjectMapper objectMapper) {
        return new SecurityAuthFilter(
                endpointRegistry,
                securitySettingsStore,
                accessPolicyEvaluator,
                clientAuthService,
                clientPermissionChecker,
                properties,
                objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public RegistrationService registrationService(
            EndpointRegistry endpointRegistry,
            RegistrationEventProducer producer,
            Environment environment) {
        return new RegistrationService(endpointRegistry, producer, environment);
    }
}
