package com.pm.sharedlib.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pm.sharedlib.config.VdtShareProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecuritySettingsStoreTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOperations;

    SecuritySettingsStore store;
    VdtShareProperties properties;

    @BeforeEach
    void setUp() {
        properties = new VdtShareProperties();
        store = new SecuritySettingsStore(redisTemplate, new ObjectMapper().registerModule(new JavaTimeModule()), properties);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void getClientApi_shouldReadConfigFromClientApiPrefixAndIgnoreClientId() {
        var endpointId = UUID.randomUUID();
        var key = "vdt:client-api:" + endpointId;
        when(valueOperations.get(key)).thenReturn("""
                {
                  "id":"%s",
                  "endpointId":"%s",
                  "destinationUrl":"https://orders.example/api",
                  "method":"POST",
                  "protocol":"HTTP",
                  "maxRetries":2,
                  "retryDelayMs":150,
                  "failureAction":"IGNORE",
                  "latencyThresholdMs":300,
                  "enabled":true,
                  "syncStatus":"ACTIVE",
                  "clientId":"%s"
                }
                """.formatted(UUID.randomUUID(), endpointId, UUID.randomUUID()));

        var result = store.getClientApi(endpointId);

        assertThat(result).isPresent();
        assertThat(result.get().getEndpointId()).isEqualTo(endpointId);
        assertThat(result.get().getDestinationUrl()).isEqualTo("https://orders.example/api");
        assertThat(result.get().getMaxRetries()).isEqualTo(2);
        assertThat(result.get().getRetryDelayMs()).isEqualTo(150);
    }

    @Test
    void getClientApi_shouldReturnEmptyWhenEndpointIdMissingOrRedisValueBlank() {
        var endpointId = UUID.randomUUID();
        when(valueOperations.get("vdt:client-api:" + endpointId)).thenReturn("");

        assertThat(store.getClientApi(null)).isEmpty();
        assertThat(store.getClientApi(endpointId)).isEmpty();
    }
}
