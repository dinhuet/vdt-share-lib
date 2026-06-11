package com.pm.sharedlib.runtime;

import com.pm.sharedlib.config.VdtShareProperties;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SigningSecretServiceTest {

    @Test
    void shouldDecryptSigningSecretEncryptedByManagementSystemAlgorithm() throws Exception {
        var properties = new VdtShareProperties();
        properties.getRuntime().setCredentialEncryptionKey("test-encryption-key");
        var service = new SigningSecretService(properties);
        var encrypted = encrypt("hmac_secret_test", "test-encryption-key");

        var result = service.decryptSigningSecret(encrypted);

        assertThat(result).isEqualTo("hmac_secret_test");
    }

    @Test
    void shouldRejectInvalidEncryptedSecretFormat() {
        var service = new SigningSecretService(new VdtShareProperties());

        assertThatThrownBy(() -> service.decryptSigningSecret("invalid"))
                .isInstanceOf(RuntimeSecurityException.class)
                .satisfies(e -> assertThat(((RuntimeSecurityException) e).getErrorCode())
                        .isEqualTo(RuntimeSecurityErrorCodes.AUTH_SIGNING_SECRET_INVALID));
    }

    private String encrypt(String signingSecret, String encryptionKey) throws Exception {
        var iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        var cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, buildKey(encryptionKey), new GCMParameterSpec(128, iv));
        var cipherText = cipher.doFinal(signingSecret.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(iv)
                + ":"
                + Base64.getUrlEncoder().withoutPadding().encodeToString(cipherText);
    }

    private SecretKeySpec buildKey(String encryptionKey) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        return new SecretKeySpec(digest.digest(encryptionKey.getBytes(StandardCharsets.UTF_8)), "AES");
    }
}
