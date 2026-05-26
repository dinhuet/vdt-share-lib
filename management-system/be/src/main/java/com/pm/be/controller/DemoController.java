package com.pm.be.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    @GetMapping("/public")
    public Map<String, Object> publicEndpoint() {
        return Map.of("message", "This is a public endpoint", "status", "ok");
    }

    @GetMapping("/secure")
    public Map<String, Object> secureEndpoint(Authentication authentication) {
        return Map.of(
                "message", "This is a secure endpoint",
                "status", "authenticated",
                "authType", authentication != null ? authentication.getClass().getSimpleName() : "none"
        );
    }

    @GetMapping("/user")
    public Map<String, Object> userEndpoint(Authentication authentication) {
        if (authentication == null) {
            return Map.of("error", "Not authenticated");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return Map.of(
                    "message", "User info from JWT",
                    "preferredUsername", jwt.getClaimAsString("preferred_username"),
                    "email", jwt.getClaimAsString("email"),
                    "subject", jwt.getSubject()
            );
        }
        return Map.of(
                "principal", principal.toString(),
                "authType", authentication.getClass().getSimpleName()
        );
    }
}
