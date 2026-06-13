package com.pm.sharedlib.runtime;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaRuntimeAuthHeadersTest {

    @Test
    void shouldExtractHeaderValue() {
        Headers headers = new RecordHeaders(new RecordHeader[]{
                new RecordHeader("X-Client-Id", "test-client".getBytes(StandardCharsets.UTF_8)),
                new RecordHeader("X-Key-Id", "test-key".getBytes(StandardCharsets.UTF_8)),
        });

        var adapter = new KafkaRuntimeAuthHeaders(headers);

        assertThat(adapter.get("X-Client-Id")).isEqualTo("test-client");
        assertThat(adapter.get("X-Key-Id")).isEqualTo("test-key");
    }

    @Test
    void shouldReturnNullForMissingHeader() {
        Headers headers = new RecordHeaders();

        var adapter = new KafkaRuntimeAuthHeaders(headers);

        assertThat(adapter.get("X-Missing-Header")).isNull();
    }

    @Test
    void shouldReturnLastHeaderWhenMultiple() {
        Headers headers = new RecordHeaders();
        headers.add("X-Client-Id", "first".getBytes(StandardCharsets.UTF_8));
        headers.add("X-Client-Id", "last".getBytes(StandardCharsets.UTF_8));

        var adapter = new KafkaRuntimeAuthHeaders(headers);

        assertThat(adapter.get("X-Client-Id")).isEqualTo("last");
    }

    @Test
    void shouldHandleEmptyHeaders() {
        var adapter = new KafkaRuntimeAuthHeaders(new RecordHeaders());

        assertThat(adapter.get("X-Client-Id")).isNull();
    }

    @Test
    void shouldHandleNullValueInHeader() {
        Headers headers = new RecordHeaders();
        headers.add("X-Client-Id", (byte[]) null);

        var adapter = new KafkaRuntimeAuthHeaders(headers);

        assertThat(adapter.get("X-Client-Id")).isNull();
    }
}
