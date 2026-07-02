package com.pm.be.service.anomaly;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.be.config.BaselineJobProperties;
import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.enums.AnomalyTimeBucketType;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ElasticsearchBaselineQueryServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void queryBucketValues_requestCountParsesStringResponseAndAvoidsUnneededScripts() throws IOException {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = startServer(requestBody, """
                {"aggregations":{"scopes":{"buckets":[{"key":"API:/checkout","windows":{"buckets":[{"doc_count":3},{"doc_count":5}]}}]}}}
                """);
        try {
            ElasticsearchBaselineQueryService service = service(server);

            var values = service.queryBucketValues(
                    "request_count_1m",
                    AnomalyScopeType.ENDPOINT,
                    7,
                    AnomalyTimeBucketType.GLOBAL,
                    null,
                    60,
                    Instant.parse("2026-07-02T16:05:00Z"));

            assertThat(values).containsEntry("API:/checkout", List.of(3.0, 5.0));
            assertThat(requestBody.get()).doesNotContain("status.keyword", "retryAttempt", "latencyThresholdMs", "slowRequest", "authFail");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void queryBucketValues_slowRequestScriptGuardsMissingOptionalFields() throws IOException {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = startServer(requestBody, """
                {"aggregations":{"scopes":{"buckets":[{"key":"API:/checkout","windows":{"buckets":[{"doc_count":4,"slowRequest":{"doc_count":1}}]}}]}}}
                """);
        try {
            ElasticsearchBaselineQueryService service = service(server);

            var values = service.queryBucketValues(
                    "slow_request_rate_5m",
                    AnomalyScopeType.ENDPOINT,
                    7,
                    AnomalyTimeBucketType.SAME_HOUR,
                    "HOUR_23",
                    300,
                    Instant.parse("2026-07-02T16:05:00Z"));

            assertThat(values).containsEntry("API:/checkout", List.of(0.25));
            assertThat(requestBody.get())
                    .contains("doc.containsKey('durationMs')", "doc.containsKey('latencyThresholdMs')", "doc.containsKey('timestamp')")
                    .doesNotContain("retryAttempt", "resultCode.keyword");
        } finally {
            server.stop(0);
        }
    }

    private ElasticsearchBaselineQueryService service(HttpServer server) {
        BaselineJobProperties properties = new BaselineJobProperties();
        properties.setElasticsearchUrl("http://localhost:" + server.getAddress().getPort());
        properties.setIndexPattern("security-logs-*");
        properties.setTimestampField("timestamp");
        return new ElasticsearchBaselineQueryService(properties, objectMapper);
    }

    private HttpServer startServer(AtomicReference<String> requestBody, String responseBody) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        return server;
    }
}
