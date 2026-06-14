package com.pm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RestController
@RequestMapping("/api/client/outbound")
public class OutboundOrderController {

    private final AtomicInteger failureAttempts = new AtomicInteger();

    @PostMapping("/orders")
    public String receiveOrder(
            @RequestBody String payload,
            @RequestParam(defaultValue = "0") int failTimes) {
        var attempt = failureAttempts.incrementAndGet();
        if (attempt <= failTimes) {
            log.warn("Simulated outbound failure attempt={} failTimes={} payload={}", attempt, failTimes, payload);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Simulated outbound failure attempt " + attempt + " of " + failTimes);
        }
        log.info("Received outbound order from vdt-demo: {}", payload);
        return "client received: " + payload;
    }

    @DeleteMapping("/failures")
    public String resetFailures() {
        failureAttempts.set(0);
        return "failure counter reset";
    }
}
