package com.pm.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaClientService {

    public static final String DEMO_TOPIC = "demo.orders";

    // ── User tự điền đúng 4 field này ─────────────────────────────────────
    public static final String CLIENT_ID = "7bb8249d-95ae-4f94-bc9d-621cf8edd588";
    public static final String KEY_ID = "key-e6c49436";
    public static final String API_KEY = "vdt_live_ezCxtEDrXlL0R9XwuWQjqw0osT_7bvnd8VGo_3bPtIc";
    public static final String SIGNING_SECRET = "vdt_live_ezCxtEDrXlL0R9XwuWQjqw0osT_7bvnd8VGo_3bPtIc";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendOrder(String payload) {
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }

        var timestamp = String.valueOf(System.currentTimeMillis());
        var nonce = UUID.randomUUID().toString();
        var bodyHash = base64UrlSha256(payload);

        // MQ canonical string used for signing:
        // topic + '\n' + clientId + '\n' + keyId + '\n' + timestampMillis
        // + '\n' + nonce + '\n' + base64url_no_padding(sha256(payloadUtf8))
        var canonical = String.join("\n",
                DEMO_TOPIC, CLIENT_ID, KEY_ID, timestamp, nonce, bodyHash);
        var signature = hmacSha256(canonical, SIGNING_SECRET);

        var headerValues = new LinkedHashMap<String, String>();
        headerValues.put("X-Client-Id", CLIENT_ID);
        headerValues.put("X-Key-Id", KEY_ID);
        headerValues.put("X-Api-Key", API_KEY);
        headerValues.put("X-Timestamp", timestamp);
        headerValues.put("X-Nonce", nonce);
        headerValues.put("X-Signature", signature);

        var headers = new RecordHeaders();
        headerValues.forEach((name, value) -> addHeader(headers, name, value));

        log.info("Kafka manual test topic: {}", DEMO_TOPIC);
        log.info("Kafka manual test payload: {}", payload);
        log.info("Generated X-Timestamp: {}", timestamp);
        log.info("Generated X-Nonce: {}", nonce);
        log.info("Generated X-Signature: {}", signature);
        log.info("Exact Kafka headers sent: {}", sentHeaders(headerValues));

        var record = new ProducerRecord<String, String>(DEMO_TOPIC, null, null, payload, headers);
        var future = kafkaTemplate.send(record);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send order to topic={}", DEMO_TOPIC, ex);
            } else {
                log.info("Sent order to topic={}, partition={}, offset={}",
                        DEMO_TOPIC, result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    static String base64UrlSha256(String input) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    static String hmacSha256(String data, String secret) {
        try {
            var algo = "HmacSHA256";
            var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algo);
            var mac = Mac.getInstance(algo);
            mac.init(key);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to generate HMAC-SHA256 signature", e);
        }
    }

    private Map<String, String> sentHeaders(Map<String, String> headerValues) {
        var sent = new LinkedHashMap<String, String>();
        headerValues.forEach((name, value) -> {
            if (value != null && !value.isBlank()) {
                sent.put(name, value);
            }
        });
        return sent;
    }

    private void addHeader(RecordHeaders headers, String name, String value) {
        if (value != null && !value.isBlank()) {
            headers.add(name, value.getBytes(StandardCharsets.UTF_8));
        }
    }
}
