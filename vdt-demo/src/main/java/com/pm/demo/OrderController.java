package com.pm.demo;

import com.pm.sharedlib.annotation.ClientCall;
import com.pm.sharedlib.annotation.SharedApi;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @SharedApi(name = "get-orders", path = "/api/orders", method = "GET")
    @GetMapping
    public List<String> getOrders() {
        return List.of("order1", "order2");
    }

    @SharedApi(name = "create-order", path = "/api/orders", method = "POST")
    @PostMapping
    public String createOrder(@RequestBody String order) {
        return "created: " + order;
    }

//    @ClientCall(name = "notify-partner", destinationUrl = "https://partner.com/webhook", method = "POST")
//    public void notifyPartner() {
//    }
}
