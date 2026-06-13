package com.pm.sharedlib.runtime;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

public class ClientAuthService {

    private static final int UNAUTHORIZED = 401;
    private static final String ACTIVE = "ACTIVE";

    private final SecuritySettingsStore settingsStore;
    private final HmacSignatureVerifier hmacSignatureVerifier;

    public ClientAuthService(SecuritySettingsStore settingsStore) {
        this(settingsStore, null);
    }

    public ClientAuthService(SecuritySettingsStore settingsStore, HmacSignatureVerifier hmacSignatureVerifier) {
        this.settingsStore = settingsStore;
        this.hmacSignatureVerifier = hmacSignatureVerifier;
    }

    public AuthenticatedClient authenticate(HttpServletRequest request) {
        var clientId = parseClientId(requiredHeader(request, RuntimeSecurityHeaders.CLIENT_ID));
        var keyId = requiredHeader(request, RuntimeSecurityHeaders.KEY_ID);
        var apiKey = requiredHeader(request, RuntimeSecurityHeaders.API_KEY);

        var credential = settingsStore.getCredential(keyId)
                .orElseThrow(() -> unauthorized(
                        RuntimeSecurityErrorCodes.AUTH_CREDENTIAL_NOT_FOUND,
                        "Client credential was not found"));

        validateCredential(credential);
        validateApiKey(apiKey, credential);
        if (!clientId.equals(credential.getClientId())) {
            throw unauthorized(RuntimeSecurityErrorCodes.AUTH_CLIENT_MISMATCH, "Client id does not match credential");
        }
        if (hmacSignatureVerifier != null) {
            hmacSignatureVerifier.verify(request, credential);
        }

        return AuthenticatedClient.builder()
                .clientId(clientId)
                .clientCode(credential.getClientCode())
                .clientName(credential.getClientName())
                .keyId(credential.getKeyId())
                .build();
    }

    public AuthenticatedClient authenticate(RuntimeAuthHeaders headers) {
        return authenticate(headers, null, null);
    }

    public AuthenticatedClient authenticate(RuntimeAuthHeaders headers, String topic, byte[] payloadBytes) {
        var clientId = parseClientId(requiredHeader(headers, RuntimeSecurityHeaders.CLIENT_ID));
        var keyId = requiredHeader(headers, RuntimeSecurityHeaders.KEY_ID);
        var apiKey = requiredHeader(headers, RuntimeSecurityHeaders.API_KEY);

        var credential = settingsStore.getCredential(keyId)
                .orElseThrow(() -> unauthorized(
                        RuntimeSecurityErrorCodes.AUTH_CREDENTIAL_NOT_FOUND,
                        "Client credential was not found"));

        validateCredential(credential);
        validateApiKey(apiKey, credential);
        if (!clientId.equals(credential.getClientId())) {
            throw unauthorized(RuntimeSecurityErrorCodes.AUTH_CLIENT_MISMATCH, "Client id does not match credential");
        }
        if (hmacSignatureVerifier != null) {
            hmacSignatureVerifier.verifyMq(headers, topic, payloadBytes, credential);
        }

        return AuthenticatedClient.builder()
                .clientId(clientId)
                .clientCode(credential.getClientCode())
                .clientName(credential.getClientName())
                .keyId(credential.getKeyId())
                .build();
    }

    private String requiredHeader(HttpServletRequest request, String headerName) {
        var value = request.getHeader(headerName);
        if (!StringUtils.hasText(value)) {
            throw unauthorized(RuntimeSecurityErrorCodes.AUTH_HEADER_MISSING, "Missing required header: " + headerName);
        }
        return value.trim();
    }

    private String requiredHeader(RuntimeAuthHeaders headers, String headerName) {
        var value = headers.get(headerName);
        if (!StringUtils.hasText(value)) {
            throw unauthorized(RuntimeSecurityErrorCodes.AUTH_HEADER_MISSING, "Missing required header: " + headerName);
        }
        return value.trim();
    }

    private UUID parseClientId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw unauthorized(RuntimeSecurityErrorCodes.AUTH_CLIENT_ID_INVALID, "Invalid X-Client-Id header");
        }
    }

    private void validateCredential(ClientCredentialRuntimeConfig credential) {
        if (!ACTIVE.equalsIgnoreCase(credential.getStatus())) {
            throw unauthorized(RuntimeSecurityErrorCodes.AUTH_CREDENTIAL_INACTIVE, "Client credential is not active");
        }
        if (credential.getRevokedAt() != null) {
            throw unauthorized(RuntimeSecurityErrorCodes.AUTH_CREDENTIAL_INACTIVE, "Client credential was revoked");
        }
        if (isExpired(credential.getExpiresAt())) {
            throw unauthorized(RuntimeSecurityErrorCodes.AUTH_CREDENTIAL_EXPIRED, "Client credential expired");
        }
    }

    private boolean isExpired(LocalDateTime expiresAt) {
        if (expiresAt == null) {
            return false;
        }
        return !expiresAt.isAfter(LocalDateTime.now());
    }

    private void validateApiKey(String apiKey, ClientCredentialRuntimeConfig credential) {
        if (!hashApiKey(apiKey).equals(credential.getApiKeyHash())) {
            throw unauthorized(RuntimeSecurityErrorCodes.AUTH_API_KEY_INVALID, "Invalid API key");
        }
    }

    private String hashApiKey(String apiKey) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw unauthorized(RuntimeSecurityErrorCodes.AUTH_API_KEY_INVALID, "Failed to hash API key");
        }
    }

    private RuntimeSecurityException unauthorized(String errorCode, String message) {
        return new RuntimeSecurityException(UNAUTHORIZED, errorCode, message);
    }
}
