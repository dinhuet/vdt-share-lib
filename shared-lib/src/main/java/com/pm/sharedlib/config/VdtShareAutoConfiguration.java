package com.pm.sharedlib.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.sharedlib.kafka.RegistrationEventProducer;
import com.pm.sharedlib.service.RegistrationService;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaTemplate;

@AutoConfiguration
@ConditionalOnProperty(prefix = "vdt.share", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(VdtShareProperties.class)
public class VdtShareAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public RegistrationEventProducer registrationEventProducer(
            KafkaTemplate<String, String> kafka,
            ObjectMapper objectMapper) {
        return new RegistrationEventProducer(kafka, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public RegistrationService registrationService(
            ListableBeanFactory beanFactory,
            RegistrationEventProducer producer,
            VdtShareProperties properties,
            Environment environment) {
        return new RegistrationService(beanFactory, producer, properties, environment);
    }
}
