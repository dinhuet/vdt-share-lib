package com.pm.be;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@SpringBootTest(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri="
})
class BeApplicationTests {

    @Test
    void contextLoads() {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestSecurityConfig {

        @Bean
        @Primary
        JwtDecoder jwtDecoder() {
            return token -> null;
        }
    }
}
