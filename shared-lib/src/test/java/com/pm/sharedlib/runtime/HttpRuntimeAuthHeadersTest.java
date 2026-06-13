package com.pm.sharedlib.runtime;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class HttpRuntimeAuthHeadersTest {

    @Test
    void shouldExtractHeaderValue() {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Client-Id", "test-client");
        request.addHeader("X-Key-Id", "test-key");

        var adapter = new HttpRuntimeAuthHeaders(request);

        assertThat(adapter.get("X-Client-Id")).isEqualTo("test-client");
        assertThat(adapter.get("X-Key-Id")).isEqualTo("test-key");
    }

    @Test
    void shouldReturnNullForMissingHeader() {
        var request = new MockHttpServletRequest();

        var adapter = new HttpRuntimeAuthHeaders(request);

        assertThat(adapter.get("X-Missing-Header")).isNull();
    }

    @Test
    void shouldReturnNullForNullHeader() {
        var request = new MockHttpServletRequest();

        var adapter = new HttpRuntimeAuthHeaders(request);

        assertThat(adapter.get("X-Client-Id")).isNull();
    }
}
