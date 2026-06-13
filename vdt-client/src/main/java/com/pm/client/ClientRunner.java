package com.pm.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientRunner implements CommandLineRunner {

    private final KafkaClientService kafkaClientService;

    @Override
    public void run(String... args) {
        var payload = args.length > 0
                ? String.join(" ", Arrays.asList(args))
                : """
                {"id":1,"item":"sách lập trình","quantity":2,"price":150000}
                """.strip();

        log.info("=== VDCLIENT gửi order lên topic '{}' ===", KafkaClientService.DEMO_TOPIC);
        log.info("Payload source: {}", args.length > 0 ? "command-line args" : "default demo payload");

        log.info("User-filled credentials:");
        log.info("  X-Client-Id      : {}", blankToText(KafkaClientService.CLIENT_ID));
        log.info("  X-Key-Id         : {}", blankToText(KafkaClientService.KEY_ID));
        log.info("  X-Api-Key        : {}", blankToText(KafkaClientService.API_KEY));
        log.info("  X-Signing-Secret : (configured, not printed)");
        log.info("Auto-generated headers: X-Timestamp, X-Nonce, X-Signature");

        if (KafkaClientService.CLIENT_ID.isBlank()) {
            log.warn("⚠  Chưa có credential — message sẽ bị từ chối nếu access policy yêu cầu auth.");
            log.warn("   Sửa hằng số trong {} để thêm credential thật.",
                    KafkaClientService.class.getSimpleName());
        }

        kafkaClientService.sendOrder(payload);
        log.info("=== Đã gửi, kiểm tra log bên vdt-demo để xem kết quả ===");
    }

    private static String blankToText(String value) {
        return value.isBlank() ? "(trống)" : value;
    }
}
