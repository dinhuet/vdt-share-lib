package com.pm.sharedlib.config;

import com.pm.sharedlib.runtime.SecurityLogEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class VdtShareAutoConfigurationTest {

    @Test
    void objectMapper_shouldSerializeInstantAsIso8601String() throws Exception {
        var objectMapper = new VdtShareAutoConfiguration().objectMapper();
        var event = SecurityLogEvent.builder()
                .timestamp(Instant.parse("2026-06-24T10:28:09.151Z"))
                .build();

        String json = objectMapper.writeValueAsString(event);

        assertThat(json).contains("\"timestamp\":\"2026-06-24T10:28:09.151Z\"");
    }
}
