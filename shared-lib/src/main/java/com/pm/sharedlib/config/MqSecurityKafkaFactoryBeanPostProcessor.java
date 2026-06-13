package com.pm.sharedlib.config;

import com.pm.sharedlib.runtime.ExposedMqSecurityInterceptor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.RecordInterceptor;

public class MqSecurityKafkaFactoryBeanPostProcessor implements BeanPostProcessor {

    private final ExposedMqSecurityInterceptor securityInterceptor;
    private final CommonErrorHandler errorHandler;

    public MqSecurityKafkaFactoryBeanPostProcessor(
            ExposedMqSecurityInterceptor securityInterceptor,
            CommonErrorHandler errorHandler) {
        this.securityInterceptor = securityInterceptor;
        this.errorHandler = errorHandler;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ConcurrentKafkaListenerContainerFactory<?, ?> factory) {
            attachSecurity(factory);
        }
        return bean;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void attachSecurity(ConcurrentKafkaListenerContainerFactory factory) {
        var existing = factory.getRecordInterceptor();
        if (existing instanceof SecurityFirstRecordInterceptor) {
            factory.setCommonErrorHandler(errorHandler);
            return;
        }

        RecordInterceptor composed = existing == null
                ? securityInterceptor
                : new SecurityFirstRecordInterceptor(securityInterceptor, existing);
        factory.setRecordInterceptor(composed);
        factory.setCommonErrorHandler(errorHandler);
    }

    private static final class SecurityFirstRecordInterceptor implements RecordInterceptor<Object, Object> {

        private final ExposedMqSecurityInterceptor securityInterceptor;
        private final RecordInterceptor<Object, Object> existingInterceptor;

        @SuppressWarnings("unchecked")
        private SecurityFirstRecordInterceptor(
                ExposedMqSecurityInterceptor securityInterceptor,
                RecordInterceptor<?, ?> existingInterceptor) {
            this.securityInterceptor = securityInterceptor;
            this.existingInterceptor = (RecordInterceptor<Object, Object>) existingInterceptor;
        }

        @Override
        public ConsumerRecord<Object, Object> intercept(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
            var securedRecord = securityInterceptor.intercept(record, consumer);
            if (securedRecord == null) {
                return null;
            }
            return existingInterceptor.intercept(securedRecord, consumer);
        }

        @Override
        public void success(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
            try {
                existingInterceptor.success(record, consumer);
            } finally {
                securityInterceptor.success(record, consumer);
            }
        }

        @Override
        public void failure(ConsumerRecord<Object, Object> record, Exception exception, Consumer<Object, Object> consumer) {
            try {
                existingInterceptor.failure(record, exception, consumer);
            } finally {
                securityInterceptor.failure(record, exception, consumer);
            }
        }

        @Override
        public void afterRecord(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
            try {
                existingInterceptor.afterRecord(record, consumer);
            } finally {
                securityInterceptor.afterRecord(record, consumer);
            }
        }
    }
}
