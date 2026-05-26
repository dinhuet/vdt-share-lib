package com.pm.be;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.keycloak.provider=nonexistent",
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
        ClientRegistrationRepository clientRegistrationRepository() {
            ClientRegistration registration = ClientRegistration.withRegistrationId("keycloak")
                    .clientId("be-app")
                    .clientSecret("test-secret")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/{action}/oauth2/code/{registrationId}")
                    .scope("openid", "profile", "email")
                    .authorizationUri("http://localhost:8080/realms/vdt-shared-lib/protocol/openid-connect/auth")
                    .tokenUri("http://localhost:8080/realms/vdt-shared-lib/protocol/openid-connect/token")
                    .userInfoUri("http://localhost:8080/realms/vdt-shared-lib/protocol/openid-connect/userinfo")
                    .jwkSetUri("http://localhost:8080/realms/vdt-shared-lib/protocol/openid-connect/certs")
                    .userNameAttributeName("preferred_username")
                    .clientName("Keycloak")
                    .build();
            return new InMemoryClientRegistrationRepository(registration);
        }

        @Bean
        @Primary
        OAuth2AuthorizedClientService oAuth2AuthorizedClientService(
                ClientRegistrationRepository clientRegistrationRepository) {
            return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
        }

        @Bean
        @Primary
        JwtDecoder jwtDecoder() {
            return token -> null;
        }
    }
}
