package com.pm.sharedlib.runtime;

import com.pm.sharedlib.config.VdtShareProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HmacSignatureVerifierTest {

    private static final String SIGNING_SECRET = "hmac_secret_test";
    private static final UUID CLIENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Mock
    StringRedisTemplate redisTemplate;
    @Mock
    ValueOperations<String, String> valueOperations;
    @Mock
    SigningSecretService signingSecretService;

    VdtShareProperties properties;
    HmacSignatureVerifier verifier;
    ClientCredentialRuntimeConfig credential;

    @BeforeEach
    void setUp() {
        properties = new VdtShareProperties();
        properties.getRuntime().setHmacEnabled(true);
        properties.getRuntime().setHmacMaxClockSkewSeconds(300);
        verifier = new HmacSignatureVerifier(redisTemplate, properties, signingSecretService);
        credential = ClientCredentialRuntimeConfig.builder()
                .clientId(CLIENT_ID)
                .keyId("key_demo_01")
                .signingSecretEncrypted("encrypted-secret")
                .algorithm("HMAC-SHA256")
                .build();
    }

    @Test
    void shouldVerifyValidSignature() throws Exception {
        var timestamp = String.valueOf(Instant.now().toEpochMilli());
        var request = signedRequest(timestamp, "nonce-1", "{\"amount\":100}");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(
                eq("vdt:hmac-nonce:key_demo_01:nonce-1"), eq("1"), eq(300L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(signingSecretService.decryptSigningSecret("encrypted-secret")).thenReturn(SIGNING_SECRET);

        verifier.verify(request, credential);
    }

    @Test
    void shouldRejectInvalidSignature() throws Exception {
        var timestamp = String.valueOf(Instant.now().toEpochMilli());
        var request = signedRequest(timestamp, "nonce-2", "{\"amount\":100}", "bad-signature");
        when(signingSecretService.decryptSigningSecret("encrypted-secret")).thenReturn(SIGNING_SECRET);

        assertThatThrownBy(() -> verifier.verify(request, credential))
                .isInstanceOf(RuntimeSecurityException.class)
                .satisfies(e -> assertThat(((RuntimeSecurityException) e).getErrorCode())
                        .isEqualTo(RuntimeSecurityErrorCodes.AUTH_SIGNATURE_INVALID));
    }

    @Test
    void shouldRejectReplayedNonce() throws Exception {
        var timestamp = String.valueOf(Instant.now().toEpochMilli());
        var request = signedRequest(timestamp, "nonce-3", "{}");
        when(signingSecretService.decryptSigningSecret("encrypted-secret")).thenReturn(SIGNING_SECRET);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(
                eq("vdt:hmac-nonce:key_demo_01:nonce-3"), eq("1"), eq(300L), eq(TimeUnit.SECONDS)))
                .thenReturn(false);

        assertThatThrownBy(() -> verifier.verify(request, credential))
                .isInstanceOf(RuntimeSecurityException.class)
                .satisfies(e -> assertThat(((RuntimeSecurityException) e).getErrorCode())
                        .isEqualTo(RuntimeSecurityErrorCodes.AUTH_NONCE_REPLAYED));
    }

    @Test
    void shouldRejectExpiredTimestamp() throws Exception {
        var timestamp = String.valueOf(Instant.now().minusSeconds(301).toEpochMilli());
        var request = signedRequest(timestamp, "nonce-4", "{}");

        assertThatThrownBy(() -> verifier.verify(request, credential))
                .isInstanceOf(RuntimeSecurityException.class)
                .satisfies(e -> assertThat(((RuntimeSecurityException) e).getErrorCode())
                        .isEqualTo(RuntimeSecurityErrorCodes.AUTH_TIMESTAMP_EXPIRED));
    }

    @Test
    void shouldVerifyValidMqSignature() throws Exception {
        var timestamp = String.valueOf(Instant.now().toEpochMilli());
        var payload = "{\"amount\":100}".getBytes(StandardCharsets.UTF_8);
        var headers = signedMqHeaders("orders.created", timestamp, "mq-nonce-1", payload);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(
                eq("vdt:hmac-nonce:key_demo_01:mq-nonce-1"), eq("1"), eq(300L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(signingSecretService.decryptSigningSecret("encrypted-secret")).thenReturn(SIGNING_SECRET);

        verifier.verifyMq(headers, "orders.created", payload, credential);
    }

    @Test
    void shouldRejectInvalidMqSignature() throws Exception {
        var timestamp = String.valueOf(Instant.now().toEpochMilli());
        var payload = "{}".getBytes(StandardCharsets.UTF_8);
        var headers = signedMqHeaders("orders.created", timestamp, "mq-nonce-2", payload, "bad-signature");
        when(signingSecretService.decryptSigningSecret("encrypted-secret")).thenReturn(SIGNING_SECRET);

        assertThatThrownBy(() -> verifier.verifyMq(headers, "orders.created", payload, credential))
                .isInstanceOf(RuntimeSecurityException.class)
                .satisfies(e -> assertThat(((RuntimeSecurityException) e).getErrorCode())
                        .isEqualTo(RuntimeSecurityErrorCodes.AUTH_SIGNATURE_INVALID));
    }

    @Test
    void shouldRejectMqMissingSignatureHeaders() {
        var headers = new KafkaRuntimeAuthHeaders(new RecordHeaders());

        assertThatThrownBy(() -> verifier.verifyMq(headers, "orders.created", new byte[0], credential))
                .isInstanceOf(RuntimeSecurityException.class)
                .satisfies(e -> assertThat(((RuntimeSecurityException) e).getErrorCode())
                        .isEqualTo(RuntimeSecurityErrorCodes.AUTH_SIGNATURE_HEADER_MISSING));
    }

    @Test
    void shouldRejectMqUnsupportedAlgorithm() throws Exception {
        credential.setAlgorithm("PLAINTEXT");
        var timestamp = String.valueOf(Instant.now().toEpochMilli());
        var payload = "{}".getBytes(StandardCharsets.UTF_8);
        var headers = signedMqHeaders("orders.created", timestamp, "mq-nonce-3", payload);

        assertThatThrownBy(() -> verifier.verifyMq(headers, "orders.created", payload, credential))
                .isInstanceOf(RuntimeSecurityException.class)
                .satisfies(e -> assertThat(((RuntimeSecurityException) e).getErrorCode())
                        .isEqualTo(RuntimeSecurityErrorCodes.AUTH_ALGORITHM_UNSUPPORTED));
    }

    @Test
    void shouldSkipMqVerificationWhenHmacDisabled() {
        properties.getRuntime().setHmacEnabled(false);

        verifier.verifyMq(new KafkaRuntimeAuthHeaders(new RecordHeaders()), "orders.created", null, credential);
    }

    private CachedBodyHttpServletRequest signedRequest(String timestamp, String nonce, String body) throws Exception {
        return signedRequest(timestamp, nonce, body, sign(timestamp, nonce, body));
    }

    private CachedBodyHttpServletRequest signedRequest(String timestamp, String nonce, String body, String signature) throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/orders");
        request.setQueryString("page=1");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        request.addHeader(RuntimeSecurityHeaders.CLIENT_ID, CLIENT_ID.toString());
        request.addHeader(RuntimeSecurityHeaders.KEY_ID, "key_demo_01");
        request.addHeader(RuntimeSecurityHeaders.TIMESTAMP, timestamp);
        request.addHeader(RuntimeSecurityHeaders.NONCE, nonce);
        request.addHeader(RuntimeSecurityHeaders.SIGNATURE, signature);
        return new CachedBodyHttpServletRequest(request);
    }

    private String sign(String timestamp, String nonce, String body) throws Exception {
        var canonical = "POST\n"
                + "/api/orders?page=1\n"
                + CLIENT_ID + "\n"
                + "key_demo_01\n"
                + timestamp + "\n"
                + nonce + "\n"
                + bodyHash(body);
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SIGNING_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
    }

    private KafkaRuntimeAuthHeaders signedMqHeaders(String topic, String timestamp, String nonce, byte[] payload) throws Exception {
        return signedMqHeaders(topic, timestamp, nonce, payload, signMq(topic, timestamp, nonce, payload));
    }

    private KafkaRuntimeAuthHeaders signedMqHeaders(
            String topic,
            String timestamp,
            String nonce,
            byte[] payload,
            String signature) {
        var headers = new RecordHeaders();
        headers.add(RuntimeSecurityHeaders.CLIENT_ID, CLIENT_ID.toString().getBytes(StandardCharsets.UTF_8));
        headers.add(RuntimeSecurityHeaders.KEY_ID, "key_demo_01".getBytes(StandardCharsets.UTF_8));
        headers.add(RuntimeSecurityHeaders.TIMESTAMP, timestamp.getBytes(StandardCharsets.UTF_8));
        headers.add(RuntimeSecurityHeaders.NONCE, nonce.getBytes(StandardCharsets.UTF_8));
        headers.add(RuntimeSecurityHeaders.SIGNATURE, signature.getBytes(StandardCharsets.UTF_8));
        return new KafkaRuntimeAuthHeaders(headers);
    }

    private String signMq(String topic, String timestamp, String nonce, byte[] payload) throws Exception {
        var canonical = topic + "\n"
                + CLIENT_ID + "\n"
                + "key_demo_01\n"
                + timestamp + "\n"
                + nonce + "\n"
                + bodyHash(payload);
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SIGNING_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
    }

    private String bodyHash(String body) throws Exception {
        return bodyHash(body.getBytes(StandardCharsets.UTF_8));
    }

    private String bodyHash(byte[] body) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(digest.digest(body));
    }
}
