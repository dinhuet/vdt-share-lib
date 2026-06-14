package com.pm.demo;

import com.pm.sharedlib.annotation.ClientCall;
import com.pm.sharedlib.annotation.SharedApi;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final VdtClientOutboundClient vdtClientOutboundClient;
    private final VdtClientMqPublisher vdtClientMqPublisher;

    public OrderController(VdtClientOutboundClient vdtClientOutboundClient, VdtClientMqPublisher vdtClientMqPublisher) {
        this.vdtClientOutboundClient = vdtClientOutboundClient;
        this.vdtClientMqPublisher = vdtClientMqPublisher;
    }

    @SharedApi(name = "get-orders", path = "/api/orders", method = "GET")
    @GetMapping
    public List<String> getOrders() {
        return List.of("order1", "order2");
    }

    @SharedApi(name = "create-order-v2", path = "/api/orders", method = "POST")
    @PostMapping
    public String createOrder(@RequestBody String order) {
        return "created: " + order;
    }

    @PostMapping("/notify-client")
    public String notifyClient(@RequestBody String order) {
        return vdtClientOutboundClient.notifyOrder(order);
    }

    @PostMapping("/notify-client-retry")
    public String notifyClientRetry(@RequestBody String order) {
        return vdtClientOutboundClient.notifyOrderWithRetry(order);
    }

    @PostMapping("/publish-client-mq")
    public String publishClientMq(@RequestBody String order) {
        vdtClientMqPublisher.publishOrder(order);
        return "published to topic " + VdtClientMqPublisher.VDT_CLIENT_ORDER_TOPIC + ": " + order;
    }

    @PostMapping("/publish-client-mq/fail")
    public String publishClientMqFailure(@RequestBody String order) {
        vdtClientMqPublisher.publishOrderFailure(order);
        return "unexpected success for topic " + VdtClientMqPublisher.VDT_CLIENT_ORDER_FAILURE_TOPIC;
    }

    @PostMapping("/publish-client-mq/timeout")
    public String publishClientMqTimeout(@RequestBody String order) {
        vdtClientMqPublisher.publishOrderTimeout(order);
        return "unexpected success for topic " + VdtClientMqPublisher.VDT_CLIENT_ORDER_TIMEOUT_TOPIC;
    }

    @PostMapping("/publish-client-mq/retry")
    public String publishClientMqRetry(
            @RequestBody String order,
            @RequestParam(defaultValue = "2") int failTimes) {
        vdtClientMqPublisher.resetRetryAttempts();
        vdtClientMqPublisher.publishOrderWithRetry(order, failTimes);
        return "published to topic " + VdtClientMqPublisher.VDT_CLIENT_ORDER_RETRY_TOPIC
                + " after simulated failures=" + failTimes + ": " + order;
    }

    @ClientCall(name = "notify-partner", destinationUrl = "https://partner.com/webhook", method = "POST")
    public void notifyPartner() {
    }
}
