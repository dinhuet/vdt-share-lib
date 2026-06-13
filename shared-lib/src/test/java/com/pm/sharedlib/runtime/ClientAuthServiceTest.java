package com.pm.sharedlib.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientAuthServiceTest {

    @Mock
    SecuritySettingsStore settingsStore;
    @Mock
    HmacSignatureVerifier hmacSignatureVerifier;

    @Test
    void shouldVerifyHmacAfterApiKeyAuthentication() throws Exception {
        var clientId = UUID.randomUUID();
        var credential = ClientCredentialRuntimeConfig.builder()
                .clientId(clientId)
                .clientCode("client-code")
                .clientName("Client")
                .keyId("key-id")
                .apiKeyHash(hashApiKey("raw-api-key"))
                .status("ACTIVE")
                .build();
        var request = new MockHttpServletRequest();
        request.addHeader(RuntimeSecurityHeaders.CLIENT_ID, clientId.toString());
        request.addHeader(RuntimeSecurityHeaders.KEY_ID, "key-id");
        request.addHeader(RuntimeSecurityHeaders.API_KEY, "raw-api-key");
        when(settingsStore.getCredential("key-id")).thenReturn(Optional.of(credential));

        var result = new ClientAuthService(settingsStore, hmacSignatureVerifier).authenticate(request);

        assertThat(result.getClientId()).isEqualTo(clientId);
        verify(hmacSignatureVerifier).verify(request, credential);
    }

    @Test
    void shouldVerifyMqHmacAfterApiKeyAuthentication() throws Exception {
        var clientId = UUID.randomUUID();
        var credential = ClientCredentialRuntimeConfig.builder()
                .clientId(clientId)
                .clientCode("client-code")
                .clientName("Client")
                .keyId("key-id")
                .apiKeyHash(hashApiKey("raw-api-key"))
                .status("ACTIVE")
                .build();
        var nativeHeaders = new RecordHeaders();
        nativeHeaders.add(RuntimeSecurityHeaders.CLIENT_ID, clientId.toString().getBytes(StandardCharsets.UTF_8));
        nativeHeaders.add(RuntimeSecurityHeaders.KEY_ID, "key-id".getBytes(StandardCharsets.UTF_8));
        nativeHeaders.add(RuntimeSecurityHeaders.API_KEY, "raw-api-key".getBytes(StandardCharsets.UTF_8));
        var headers = new KafkaRuntimeAuthHeaders(nativeHeaders);
        var payload = "payload".getBytes(StandardCharsets.UTF_8);
        when(settingsStore.getCredential("key-id")).thenReturn(Optional.of(credential));

        var result = new ClientAuthService(settingsStore, hmacSignatureVerifier)
                .authenticate(headers, "orders.created", payload);

        assertThat(result.getClientId()).isEqualTo(clientId);
        verify(hmacSignatureVerifier).verifyMq(eq(headers), eq("orders.created"), eq(payload), eq(credential));
    }

    private String hashApiKey(String apiKey) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(digest.digest(apiKey.getBytes(StandardCharsets.UTF_8)));
    }
}
