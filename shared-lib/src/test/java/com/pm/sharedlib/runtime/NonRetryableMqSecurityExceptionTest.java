package com.pm.sharedlib.runtime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NonRetryableMqSecurityExceptionTest {

    @Test
    void shouldConstructWithStatusCodeErrorCodeAndMessage() {
        var ex = new NonRetryableMqSecurityException(401, "AUTH_FAILED", "Authentication failed");

        assertThat(ex.getStatusCode()).isEqualTo(401);
        assertThat(ex.getErrorCode()).isEqualTo("AUTH_FAILED");
        assertThat(ex.getMessage()).isEqualTo("Authentication failed");
    }

    @Test
    void shouldBeInstanceOfRuntimeSecurityException() {
        var ex = new NonRetryableMqSecurityException(403, "PERMISSION_DENIED", "Permission denied");

        assertThat(ex).isInstanceOf(RuntimeSecurityException.class);
    }
}
