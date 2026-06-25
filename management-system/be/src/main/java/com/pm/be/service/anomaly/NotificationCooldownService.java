package com.pm.be.service.anomaly;

import com.pm.be.enums.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationCooldownService {
    private final StringRedisTemplate redisTemplate;

    public boolean reserve(UUID notificationRuleId, String alertType, String fingerprint, NotificationChannel channel, String recipient, int cooldownMinutes) {
        String key = buildKey(notificationRuleId, alertType, fingerprint, channel, recipient);
        try {
            Boolean reserved = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofMinutes(Math.max(1, cooldownMinutes)));
            return Boolean.TRUE.equals(reserved);
        } catch (RuntimeException e) {
            log.warn("Failed to reserve notification cooldown; notification will be allowed: key={}", key, e);
            return true;
        }
    }

    String buildKey(UUID notificationRuleId, String alertType, String fingerprint, NotificationChannel channel, String recipient) {
        return "vdt:alert-cooldown:"
                + (notificationRuleId == null ? "fallback" : notificationRuleId)
                + ":" + safe(alertType)
                + ":" + safe(fingerprint)
                + ":" + channel.name()
                + ":" + hash(safe(recipient));
    }

    private String safe(String value) {
        return value == null ? "unknown" : value.trim();
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest is not available", e);
        }
    }
}
