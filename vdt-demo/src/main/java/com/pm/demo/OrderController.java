package com.pm.demo;

import com.pm.sharedlib.annotation.ClientCall;
import com.pm.sharedlib.annotation.SharedApi;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final VdtClientOutboundClient vdtClientOutboundClient;

    public OrderController(VdtClientOutboundClient vdtClientOutboundClient) {
        this.vdtClientOutboundClient = vdtClientOutboundClient;
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

    @ClientCall(name = "notify-partner", destinationUrl = "https://partner.com/webhook", method = "POST")
    public void notifyPartner() {
    }
}
