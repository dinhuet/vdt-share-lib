package com.pm.be.service.anomaly;

import com.fasterxml.jackson.databind.JsonNode;
import com.pm.be.config.BaselineJobProperties;
import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.enums.AnomalyTimeBucketType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchBaselineQueryService {
    private static final Set<String> SUPPORTED_METRICS = Set.of(
            "request_count_1m", "error_rate_5m", "denied_rate_5m", "timeout_rate_5m",
            "retry_rate_5m", "p95_duration_5m", "auth_fail_rate_5m");

    private final BaselineJobProperties properties;

    public Map<String, List<Double>> queryBucketValues(String metric, AnomalyScopeType scopeType, int historyDays,
                                                       AnomalyTimeBucketType timeBucketType, String timeBucket,
                                                       int windowSeconds, Instant now) {
        if (!SUPPORTED_METRICS.contains(metric) || !isSupportedScope(scopeType)) {
            return Map.of();
        }
        Instant end = now.truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
        Instant start = end.minus(Duration.ofDays(historyDays));
        Map<String, Object> request = buildRequest(metric, scopeType, timeBucketType, timeBucket, windowSeconds, start, end);
        String path = "/" + UriUtils.encodePathSegment(indexPattern(), StandardCharsets.UTF_8) + "/_search";
        try {
            JsonNode response = RestClient.create(baseUrl()).post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);
            return parse(metric, response);
        } catch (RuntimeException e) {
            log.warn("Failed to query Elasticsearch baseline metric={} scopeType={} timeBucket={}: {}", metric, scopeType, timeBucket, e.getMessage());
            return Map.of();
        }
    }

    private Map<String, Object> buildRequest(String metric, AnomalyScopeType scopeType, AnomalyTimeBucketType timeBucketType,
                                             String timeBucket, int windowSeconds, Instant start, Instant end) {
        List<Object> filters = new ArrayList<>();
        filters.add(Map.of("range", Map.of(timestampField(), Map.of("gte", start.toString(), "lt", end.toString()))));
        filters.add(Map.of("exists", Map.of("field", "endpointId")));
        if (scopeType == AnomalyScopeType.ENDPOINT_CLIENT) {
            filters.add(Map.of("exists", Map.of("field", "clientId")));
        }
        if ("p95_duration_5m".equals(metric)) {
            filters.add(Map.of("exists", Map.of("field", "durationMs")));
        }
        if (timeBucketType == AnomalyTimeBucketType.SAME_HOUR && timeBucket != null && timeBucket.startsWith("HOUR_")) {
            filters.add(Map.of("script", Map.of("script", Map.of("source",
                    "doc['" + timestampField() + "'].value.getHour() == params.hour", "params", Map.of("hour", Integer.parseInt(timeBucket.substring(5)))))));
        }
        Map<String, Object> bucketsAgg = new LinkedHashMap<>();
        bucketsAgg.put("date_histogram", Map.of("field", timestampField(), "fixed_interval", windowSeconds + "s", "min_doc_count", 1));
        bucketsAgg.put("aggs", metricAggregations(metric));

        Map<String, Object> scopeAgg = new LinkedHashMap<>();
        scopeAgg.put("terms", Map.of("script", scopeScript(scopeType), "size", maxScopes()));
        scopeAgg.put("aggs", Map.of("windows", bucketsAgg));

        return Map.of("size", 0, "query", Map.of("bool", Map.of("filter", filters)), "aggs", Map.of("scopes", scopeAgg));
    }

    private Map<String, Object> metricAggregations(String metric) {
        if ("p95_duration_5m".equals(metric)) {
            return Map.of("duration_p95", Map.of("percentiles", Map.of("field", "durationMs", "percents", List.of(95))));
        }
        return Map.of(
                "failed", filter("doc['status.keyword'].value == 'FAILED' || doc['status.keyword'].value == 'TIMEOUT'"),
                "denied", filter("doc['status.keyword'].value == 'DENIED'"),
                "timeout", filter("doc['status.keyword'].value == 'TIMEOUT' || (!doc['resultCode.keyword'].empty && doc['resultCode.keyword'].value == 'TIMEOUT_EXCEEDED')"),
                "retry", filter("doc['status.keyword'].value == 'RETRY' || (!doc['resultCode.keyword'].empty && doc['resultCode.keyword'].value == 'RETRY_SCHEDULED') || (!doc['retryAttempt'].empty && doc['retryAttempt'].value > 1)"),
                "authFail", filter("!doc['resultCode.keyword'].empty && doc['resultCode.keyword'].value.startsWith('AUTH_') && doc['resultCode.keyword'].value != 'AUTH_NONCE_REPLAYED'")
        );
    }

    private Map<String, Object> filter(String source) {
        return Map.of("filter", Map.of("script", Map.of("script", source)));
    }

    private Map<String, Object> scopeScript(AnomalyScopeType scopeType) {
        String endpoint = "doc['flowType.keyword'].value + ':' + doc['endpointId.keyword'].value";
        String client = endpoint + " + ':client:' + doc['clientId.keyword'].value";
        return Map.of("source", scopeType == AnomalyScopeType.ENDPOINT_CLIENT ? client : endpoint);
    }

    private Map<String, List<Double>> parse(String metric, JsonNode response) {
        Map<String, List<Double>> valuesByScope = new LinkedHashMap<>();
        JsonNode scopes = response == null ? null : response.path("aggregations").path("scopes").path("buckets");
        if (scopes == null || !scopes.isArray()) {
            return Map.of();
        }
        for (JsonNode scope : scopes) {
            String scopeKey = scope.path("key").asText(null);
            if (scopeKey == null) {
                continue;
            }
            List<Double> values = new ArrayList<>();
            for (JsonNode window : scope.path("windows").path("buckets")) {
                double requestCount = window.path("doc_count").asDouble();
                double value = metricValue(metric, window, requestCount);
                if (Double.isFinite(value)) {
                    values.add(value);
                }
            }
            if (!values.isEmpty()) {
                valuesByScope.put(scopeKey, values);
            }
        }
        return valuesByScope;
    }

    private double metricValue(String metric, JsonNode window, double requestCount) {
        return switch (metric) {
            case "request_count_1m" -> requestCount;
            case "error_rate_5m" -> ratio(window.path("failed").path("doc_count").asDouble(), requestCount);
            case "denied_rate_5m" -> ratio(window.path("denied").path("doc_count").asDouble(), requestCount);
            case "timeout_rate_5m" -> ratio(window.path("timeout").path("doc_count").asDouble(), requestCount);
            case "retry_rate_5m" -> ratio(window.path("retry").path("doc_count").asDouble(), requestCount);
            case "auth_fail_rate_5m" -> ratio(window.path("authFail").path("doc_count").asDouble(), requestCount);
            case "p95_duration_5m" -> window.path("duration_p95").path("values").path("95.0").asDouble(Double.NaN);
            default -> Double.NaN;
        };
    }

    private double ratio(double numerator, double denominator) {
        return denominator <= 0 ? Double.NaN : numerator / denominator;
    }

    private boolean isSupportedScope(AnomalyScopeType scopeType) {
        return scopeType == AnomalyScopeType.ENDPOINT || scopeType == AnomalyScopeType.ENDPOINT_CLIENT;
    }

    private String baseUrl() { return properties.getElasticsearchUrl() == null ? "http://localhost:9200" : properties.getElasticsearchUrl(); }
    private String indexPattern() { return properties.getIndexPattern() == null ? "security-logs-*" : properties.getIndexPattern(); }
    private String timestampField() { return properties.getTimestampField() == null ? "timestamp" : properties.getTimestampField(); }
    private int maxScopes() { return properties.getMaxScopes() == null || properties.getMaxScopes() <= 0 ? 500 : properties.getMaxScopes(); }
}
