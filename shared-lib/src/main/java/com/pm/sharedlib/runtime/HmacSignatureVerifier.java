package com.pm.sharedlib.runtime;

import com.pm.sharedlib.config.VdtShareProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class HmacSignatureVerifier {

    private static final int UNAUTHORIZED = 401;
    private static final String HMAC_SHA256 = "HMAC-SHA256";
    private static final String MAC_ALGORITHM = "HmacSHA256";

    private final StringRedisTemplate redisTemplate;
    private final VdtShareProperties properties;
    private final SigningSecretService signingSecretService;

    public void verify(HttpServletRequest request, ClientCredentialRuntimeConfig credential) {
        if (!properties.getRuntime().isHmacEnabled()) {
            return;
        }
        if (credential == null || !HMAC_SHA256.equalsIgnoreCase(credential.getAlgorithm())) {
            throw unauthorized(RuntimeSecurityErrorCodes.AUTH_ALGORITHM_UNSUPPORTED, "Unsupported credential signing algorithm");
        }

        var timestamp = requiredHeader(request, RuntimeSecurityHeaders.TIMESTAMP);
        var nonce = requiredHeader(request, RuntimeSecurityHeaders.NONCE);
        var signature = requiredHeader(request, RuntimeSecurityHeaders.SIGNATURE);

        validateTimestamp(timestamp);

        var signingSecret = signingSecretService.decryptSigningSecret(credential.getSigningSecretEncrypted());
        var expectedSignature = sign(buildCanonicalString(request, timestamp, nonce), signingSecret);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
            throw unauthorized(RuntimeSecurityErrorCodes.AUTH_SIGNATURE_INVALID, "Invalid request signature");
        }
        checkNonce(credential.getKeyId(), nonce);
    }

    public void verifyMq(RuntimeAuthHeaders headers, String topic, byte[] payloadBytes, ClientCredentialRuntimeConfig credential) {
        if (!properties.getRuntime().isHmacEnabled()) {
            return;
        }
        if (credential == null || !HMAC_SHA256.equalsIgnoreCase(credential.getAlgorithm())) {
            throw unauthorized(RuntimeSecurityErrorCodes.AUTH_ALGORITHM_UNSUPPORTED, "Unsupported credential signing algorithm");
        }
        if (!StringUtils.hasText(topic)) {
            throw unauthorized(RuntimeSecurityErrorCodes.AUTH_SIGNATURE_INVALID, "Missing Kafka topic for signature verification");
        }
        if (payloadBytes == null) {
            throw unauthorized(RuntimeSecurityErrorCodes.AUTH_SIGNATURE_INVALID, "Missing Kafka payload bytes for signature verification");
        }

        var timestamp = requiredHeader(headers, RuntimeSecurityHeaders.TIMESTAMP);
        var nonce = requiredHeader(headers, RuntimeSecurityHeaders.NONCE);
        var signature = requiredHeader(headers, RuntimeSecurityHeaders.SIGNATURE);

        validateTimestamp(timestamp);

        var signingSecret = signingSecretService.decryptSigningSecret(credential.getSigningSecretEncrypted());
        var expectedSignature = sign(buildMqCanonicalString(headers, topic, timestamp, nonce, payloadBytes), signingSecret);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
            throw unauthorized(RuntimeSecurityErrorCodes.AUTH_SIGNATURE_INVALID, "Invalid request signature");
        }
        checkNonce(credential.getKeyId(), nonce);
    }

    private String requiredHeader(HttpServletRequest request, String headerName) {
        var value = request.getHeader(headerName);
        if (!StringUtils.hasText(value)) {
            throw unauthorized(RuntimeSecurityErrorCodes.AUTH_SIGNATURE_HEADER_MISSING, "Missing required header: " + headerName);
        }
        return value.trim();
    }

    private String requiredHeader(RuntimeAuthHeaders headers, String headerName) {
        var value = headers.get(headerName);
        if (!StringUtils.hasText(value)) {
            throw unauthorized(RuntimeSecurityErrorCodes.AUTH_SIGNATURE_HEADER_MISSING, "Missing required header: " + headerName);
        }
        return value.trim();
    }

    private void validateTimestamp(String timestampHeader) {
        long timestampMillis;
        try {
            timestampMillis = Long.parseLong(timestampHeader);
        } catch (NumberFormatException e) {
            throw unauthorized(RuntimeSecurityErrorCodes.AUTH_TIMESTAMP_INVALID, "Invalid X-Timestamp header");
        }

        var nowMillis = Instant.now().toEpochMilli();
        var allowedSkewMillis = properties.getRuntime().getHmacMaxClockSkewSeconds() * 1000L;
        if (Math.abs(nowMillis - timestampMillis) > allowedSkewMillis) {
            throw unauthorized(RuntimeSecurityErrorCodes.AUTH_TIMESTAMP_EXPIRED, "Request timestamp is outside the allowed window");
        }
    }

    private void checkNonce(String keyId, String nonce) {
        var ttl = properties.getRuntime().getHmacMaxClockSkewSeconds();
        var key = properties.getRuntime().getNonceKeyPrefix() + ":" + keyId + ":" + nonce;
        try {
            Boolean stored = redisTemplate.opsForValue().setIfAbsent(key, "1", ttl, TimeUnit.SECONDS);
            if (!Boolean.TRUE.equals(stored)) {
                throw unauthorized(RuntimeSecurityErrorCodes.AUTH_NONCE_REPLAYED, "Request nonce was already used");
            }
        } catch (RuntimeSecurityException e) {
            throw e;
        } catch (RuntimeException e) {
            if (properties.getRuntime().isFailOpen()) {
                return;
            }
            throw new IllegalStateException("Failed to check HMAC nonce in Redis: " + key, e);
        }
    }

    private String buildCanonicalString(HttpServletRequest request, String timestamp, String nonce) {
        return request.getMethod().toUpperCase(Locale.ROOT) + "\n"
                + resolvePathWithQuery(request) + "\n"
                + requiredHeader(request, RuntimeSecurityHeaders.CLIENT_ID) + "\n"
                + requiredHeader(request, RuntimeSecurityHeaders.KEY_ID) + "\n"
                + timestamp + "\n"
                + nonce + "\n"
                + hashBody(request);
    }

    private String buildMqCanonicalString(RuntimeAuthHeaders headers, String topic, String timestamp, String nonce, byte[] payloadBytes) {
        return topic + "\n"
                + requiredHeader(headers, RuntimeSecurityHeaders.CLIENT_ID) + "\n"
                + requiredHeader(headers, RuntimeSecurityHeaders.KEY_ID) + "\n"
                + timestamp + "\n"
                + nonce + "\n"
                + hashBytes(payloadBytes);
    }

    private String resolvePathWithQuery(HttpServletRequest request) {
        var path = request.getRequestURI();
        var contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        var query = request.getQueryString();
        return StringUtils.hasText(query) ? path + "?" + query : path;
    }

    private String hashBody(HttpServletRequest request) {
        try {
            var body = request instanceof CachedBodyHttpServletRequest cachedRequest
                    ? cachedRequest.getCachedBody()
                    : request.getInputStream().readAllBytes();
            var digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(body));
        } catch (IOException e) {
            throw unauthorized(RuntimeSecurityErrorCodes.AUTH_SIGNATURE_INVALID, "Failed to read request body");
        } catch (Exception e) {
            throw unauthorized(RuntimeSecurityErrorCodes.AUTH_SIGNATURE_INVALID, "Failed to hash request body");
        }
    }

    private String hashBytes(byte[] body) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(body));
        } catch (Exception e) {
            throw unauthorized(RuntimeSecurityErrorCodes.AUTH_SIGNATURE_INVALID, "Failed to hash request body");
        }
    }

    private String sign(String canonicalString, String signingSecret) {
        try {
            var mac = Mac.getInstance(MAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), MAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(canonicalString.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw unauthorized(RuntimeSecurityErrorCodes.AUTH_SIGNATURE_INVALID, "Failed to sign request");
        }
    }

    private RuntimeSecurityException unauthorized(String errorCode, String message) {
        return new RuntimeSecurityException(UNAUTHORIZED, errorCode, message);
    }
}
