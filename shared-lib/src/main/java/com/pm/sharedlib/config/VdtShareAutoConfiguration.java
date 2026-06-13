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
import com.pm.sharedlib.runtime.ExposedMqSecurityInterceptor;
import com.pm.sharedlib.runtime.HmacSignatureVerifier;
import com.pm.sharedlib.runtime.MaxResponseSizeAdvice;
import com.pm.sharedlib.runtime.MqSecurityErrorHandler;
import com.pm.sharedlib.runtime.RateLimiter;
import com.pm.sharedlib.runtime.SecurityAuthFilter;
import com.pm.sharedlib.runtime.SecuritySettingsStore;
import com.pm.sharedlib.runtime.SigningSecretService;
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
import org.springframework.kafka.listener.CommonErrorHandler;
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
    public SigningSecretService signingSecretService(VdtShareProperties properties) {
        return new SigningSecretService(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public HmacSignatureVerifier hmacSignatureVerifier(
            StringRedisTemplate redisTemplate,
            VdtShareProperties properties,
            SigningSecretService signingSecretService) {
        return new HmacSignatureVerifier(redisTemplate, properties, signingSecretService);
    }

    @Bean
    @ConditionalOnMissingBean
    public ClientAuthService clientAuthService(
            SecuritySettingsStore securitySettingsStore,
            HmacSignatureVerifier hmacSignatureVerifier) {
        return new ClientAuthService(securitySettingsStore, hmacSignatureVerifier);
    }

    @Bean
    @ConditionalOnMissingBean
    public ClientPermissionChecker clientPermissionChecker(SecuritySettingsStore securitySettingsStore) {
        return new ClientPermissionChecker(securitySettingsStore);
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimiter rateLimiter(StringRedisTemplate redisTemplate, VdtShareProperties properties) {
        return new RateLimiter(redisTemplate, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "vdt.share.runtime", name = "mq-interceptor-enabled", havingValue = "true", matchIfMissing = true)
    public ExposedMqSecurityInterceptor exposedMqSecurityInterceptor(
            EndpointRegistry endpointRegistry,
            SecuritySettingsStore securitySettingsStore,
            AccessPolicyEvaluator accessPolicyEvaluator,
            ClientAuthService clientAuthService,
            ClientPermissionChecker clientPermissionChecker,
            RateLimiter rateLimiter) {
        return new ExposedMqSecurityInterceptor(
                endpointRegistry,
                securitySettingsStore,
                accessPolicyEvaluator,
                clientAuthService,
                clientPermissionChecker,
                rateLimiter);
    }

    @Bean
    @ConditionalOnMissingBean
    public CommonErrorHandler mqSecurityErrorHandler() {
        return MqSecurityErrorHandler.create();
    }

    @Bean
    @ConditionalOnProperty(prefix = "vdt.share.runtime", name = "mq-interceptor-enabled", havingValue = "true", matchIfMissing = true)
    public MqSecurityKafkaFactoryBeanPostProcessor mqSecurityKafkaFactoryBeanPostProcessor(
            ExposedMqSecurityInterceptor exposedMqSecurityInterceptor,
            CommonErrorHandler mqSecurityErrorHandler) {
        return new MqSecurityKafkaFactoryBeanPostProcessor(exposedMqSecurityInterceptor, mqSecurityErrorHandler);
    }

    @Bean
    @ConditionalOnMissingBean
    public MaxResponseSizeAdvice maxResponseSizeAdvice(ObjectMapper objectMapper) {
        return new MaxResponseSizeAdvice(objectMapper);
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
            RateLimiter rateLimiter,
            VdtShareProperties properties,
            ObjectMapper objectMapper) {
        return new SecurityAuthFilter(
                endpointRegistry,
                securitySettingsStore,
                accessPolicyEvaluator,
                clientAuthService,
                clientPermissionChecker,
                rateLimiter,
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
