package com.pm.sharedlib.runtime;

import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

public class MqSecurityErrorHandler {

    public static CommonErrorHandler create() {
        var handler = new DefaultErrorHandler(
                new FixedBackOff(1000L, 3)
        );

        handler.addNotRetryableExceptions(NonRetryableMqSecurityException.class);

        return handler;
    }
}
