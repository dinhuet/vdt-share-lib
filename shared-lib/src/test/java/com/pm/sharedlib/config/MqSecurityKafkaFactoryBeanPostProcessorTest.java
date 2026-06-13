package com.pm.sharedlib.config;

import com.pm.sharedlib.runtime.ExposedMqSecurityInterceptor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.RecordInterceptor;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MqSecurityKafkaFactoryBeanPostProcessorTest {

    @Test
    void postProcessAfterInitialization_shouldComposeSecurityBeforeExistingInterceptor() {
        var calls = new ArrayList<String>();
        var security = new OrderingSecurityInterceptor(calls);
        var existing = new OrderingRecordInterceptor("existing", calls);
        var factory = new ConcurrentKafkaListenerContainerFactory<Object, Object>();
        factory.setRecordInterceptor(existing);
        var processor = new MqSecurityKafkaFactoryBeanPostProcessor(security, mock(CommonErrorHandler.class));
        var record = new ConsumerRecord<Object, Object>("orders.created", 0, 0L, null, "payload");

        processor.postProcessAfterInitialization(factory, "kafkaListenerContainerFactory");
        factory.getRecordInterceptor().intercept(record, mock(Consumer.class));
        factory.getRecordInterceptor().success(record, mock(Consumer.class));

        assertThat(calls).containsExactly("security.intercept", "existing.intercept", "existing.success", "security.success");
    }

    private static final class OrderingSecurityInterceptor extends ExposedMqSecurityInterceptor {
        private final ArrayList<String> calls;

        private OrderingSecurityInterceptor(ArrayList<String> calls) {
            super(null, null, null, null, null, null);
            this.calls = calls;
        }

        @Override
        public ConsumerRecord<Object, Object> intercept(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
            calls.add("security.intercept");
            return record;
        }

        @Override
        public void success(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
            calls.add("security.success");
        }
    }

    private record OrderingRecordInterceptor(String name, ArrayList<String> calls) implements RecordInterceptor<Object, Object> {
        @Override
        public ConsumerRecord<Object, Object> intercept(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
            calls.add(name + ".intercept");
            return record;
        }

        @Override
        public void success(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
            calls.add(name + ".success");
        }
    }
}
