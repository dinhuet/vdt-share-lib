package com.pm.demo;

import com.pm.sharedlib.annotation.ClientCall;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class VdtClientOutboundClient {

    static final String DESTINATION_URL = "http://localhost:8083/api/client/outbound/orders";
    static final String RETRY_DESTINATION_URL = "http://localhost:8083/api/client/outbound/orders?failTimes=3";

    private final RestClient restClient;

    public VdtClientOutboundClient() {
        this.restClient = RestClient.create();
    }

    @ClientCall(name = "notify-vdt-client", destinationUrl = DESTINATION_URL, method = "POST")
    public String notifyOrder(String payload) {
        return restClient.post()
                .uri(DESTINATION_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);
    }

    @ClientCall(name = "notify-vdt-client-retry", destinationUrl = RETRY_DESTINATION_URL, method = "POST")
    public String notifyOrderWithRetry(String payload) {
        return restClient.post()
                .uri(RETRY_DESTINATION_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);
    }
}
