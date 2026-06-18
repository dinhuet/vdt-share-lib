package com.pm.sharedlib.endpoint;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EndpointIdResolverTest {

    @Test
    void resolve_shouldReturnSameIdForSameServiceAndEndpointKey() {
        var firstResolver = new EndpointIdResolver("order-service");
        var secondResolver = new EndpointIdResolver("order-service");
        var endpointKey = "EXPOSED:HTTP:POST:/api/orders";

        var firstId = firstResolver.resolve(endpointKey);
        var secondId = secondResolver.resolve(endpointKey);

        assertThat(firstId).isEqualTo(secondId);
    }

    @Test
    void resolve_shouldReturnDifferentIdsForDifferentServicesWithSameEndpointKey() {
        var endpointKey = "EXPOSED:HTTP:POST:/api/orders";

        var orderServiceId = new EndpointIdResolver("order-service").resolve(endpointKey);
        var paymentServiceId = new EndpointIdResolver("payment-service").resolve(endpointKey);

        assertThat(orderServiceId).isNotEqualTo(paymentServiceId);
    }

    @Test
    void resolve_shouldReturnDifferentIdsForDifferentEndpointKeysInSameService() {
        var resolver = new EndpointIdResolver("order-service");

        var createOrderId = resolver.resolve("EXPOSED:HTTP:POST:/api/orders");
        var getOrdersId = resolver.resolve("EXPOSED:HTTP:GET:/api/orders");

        assertThat(createOrderId).isNotEqualTo(getOrdersId);
    }
}
