package com.pm.sharedlib.runtime;

import com.pm.sharedlib.config.VdtShareProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@RequiredArgsConstructor
public class SigningSecretService {

    private static final int UNAUTHORIZED = 401;
    private static final int GCM_TAG_BITS = 128;

    private final VdtShareProperties properties;

    public String decryptSigningSecret(String encryptedSecret) {
        if (!StringUtils.hasText(properties.getRuntime().getCredentialEncryptionKey())) {
            throw invalidSecret();
        }
        if (!StringUtils.hasText(encryptedSecret)) {
            throw invalidSecret();
        }

        var parts = encryptedSecret.split(":", -1);
        if (parts.length != 2 || !StringUtils.hasText(parts[0]) || !StringUtils.hasText(parts[1])) {
            throw invalidSecret();
        }

        try {
            var iv = Base64.getUrlDecoder().decode(parts[0]);
            var cipherText = Base64.getUrlDecoder().decode(parts[1]);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, buildKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw invalidSecret();
        }
    }

    private SecretKeySpec buildKey() throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        var key = digest.digest(properties.getRuntime().getCredentialEncryptionKey().getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(key, "AES");
    }

    private RuntimeSecurityException invalidSecret() {
        return new RuntimeSecurityException(
                UNAUTHORIZED,
                RuntimeSecurityErrorCodes.AUTH_SIGNING_SECRET_INVALID,
                "Signing secret is invalid");
    }
}
