package com.pm.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientRunner implements CommandLineRunner {

    private final KafkaClientService kafkaClientService;

    @Override
    public void run(String... args) {
        var payload = """
                {"id":1,"item":"sách lập trình","quantity":2,"price":150000}
                """.strip();

        log.info("=== VDCLIENT gửi order lên topic '{}' ===", KafkaClientService.DEMO_TOPIC);
        log.info("Payload: {}", payload);

        log.info("Security headers:");
        log.info("  X-Client-Id      : {}", blankToText(KafkaClientService.CLIENT_ID));
        log.info("  X-Key-Id         : {}", blankToText(KafkaClientService.KEY_ID));
        log.info("  X-Api-Key        : {}", blankToText(KafkaClientService.API_KEY));
        log.info("  X-Signing-Secret : {}", blankToText(KafkaClientService.SIGNING_SECRET));
        log.info("  X-Timestamp      : (auto-generated)");
        log.info("  X-Nonce          : (auto-generated)");
        log.info("  X-Signature      : (auto-generated from HMAC-SHA256)");
        log.info("  X-Correlation-Id : (auto-generated)");

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
