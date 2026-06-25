package com.pm.be.service.anomaly;

import com.pm.be.enums.NotificationChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationCooldownServiceTest {
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOperations;

    @Test
    void reserve_noExistingKey_shouldAllowAndSetKey() {
        NotificationCooldownService service = new NotificationCooldownService(redisTemplate);
        UUID ruleId = UUID.randomUUID();
        String key = service.buildKey(ruleId, "AUTH_BRUTE_FORCE", "fp", NotificationChannel.EMAIL, "admin@example.com");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(key), eq("1"), eq(Duration.ofMinutes(15)))).thenReturn(true);

        boolean allowed = service.reserve(ruleId, "AUTH_BRUTE_FORCE", "fp", NotificationChannel.EMAIL, "admin@example.com", 15);

        assertThat(allowed).isTrue();
    }

    @Test
    void reserve_existingKey_shouldSkipCooldown() {
        NotificationCooldownService service = new NotificationCooldownService(redisTemplate);
        UUID ruleId = UUID.randomUUID();
        String key = service.buildKey(ruleId, "AUTH_BRUTE_FORCE", "fp", NotificationChannel.EMAIL, "admin@example.com");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(key), eq("1"), eq(Duration.ofMinutes(15)))).thenReturn(false);

        boolean allowed = service.reserve(ruleId, "AUTH_BRUTE_FORCE", "fp", NotificationChannel.EMAIL, "admin@example.com", 15);

        assertThat(allowed).isFalse();
    }
}
