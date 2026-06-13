package com.pm.sharedlib.runtime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RetryableMqSecurityExceptionTest {

    @Test
    void shouldConstructWithStatusCodeErrorCodeAndMessage() {
        var ex = new RetryableMqSecurityException(503, "RUNTIME_ERROR", "Redis runtime error");

        assertThat(ex.getStatusCode()).isEqualTo(503);
        assertThat(ex.getErrorCode()).isEqualTo("RUNTIME_ERROR");
        assertThat(ex.getMessage()).isEqualTo("Redis runtime error");
    }

    @Test
    void shouldBeInstanceOfRuntimeSecurityException() {
        var ex = new RetryableMqSecurityException(503, "RUNTIME_ERROR", "Runtime error");

        assertThat(ex).isInstanceOf(RuntimeSecurityException.class);
    }
}
