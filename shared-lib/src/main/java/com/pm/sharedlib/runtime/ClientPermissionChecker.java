package com.pm.sharedlib.runtime;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class ClientPermissionChecker {

    private static final int FORBIDDEN = 403;

    private final SecuritySettingsStore settingsStore;

    public void checkPermission(UUID clientId, UUID exposedApiId) {
        var permission = settingsStore.getClientPermission(clientId, exposedApiId)
                .orElseThrow(() -> forbidden("Client is not allowed to access this exposed API"));

        if (!Boolean.TRUE.equals(permission.getEnabled()) || !exposedApiId.equals(permission.getExposedApiId())) {
            throw forbidden("Client permission is disabled or invalid");
        }
    }

    private RuntimeSecurityException forbidden(String message) {
        return new RuntimeSecurityException(FORBIDDEN, RuntimeSecurityErrorCodes.PERMISSION_DENIED, message);
    }
}
