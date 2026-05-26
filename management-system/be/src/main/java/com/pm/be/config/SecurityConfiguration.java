package com.pm.be.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
@ComponentScan(basePackages = "com.pm.be")
public class SecurityConfiguration {
    private final String[] PUBLIC_ENDPOINTS = {
            "/auth/introspect", "/central/api/configs/test", "/api/demo/public", "/error"};

    private final String[] STATIC_RESOURCES = {
            "/", "/index.html", "/favicon.ico", "/assets/**"};

    private final JwtConverter jwtAuthConverter;

    public SecurityConfiguration(JwtConverter jwtAuthConverter) {
        this.jwtAuthConverter = jwtAuthConverter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(STATIC_RESOURCES).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(Customizer.withDefaults())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
                )
                .logout(logout -> logout
                        .logoutSuccessHandler(keycloakLogoutSuccessHandler())
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                );
        return http.build();
    }

    private LogoutSuccessHandler keycloakLogoutSuccessHandler() {
        return (request, response, authentication) -> {
            String idToken = (authentication != null && authentication.getDetails() != null)
                    ? (String) ((java.util.Map<?, ?>) authentication.getDetails()).get("id_token")
                    : null;
            String logoutUrl = "http://localhost:8080/realms/vdt-shared-lib/protocol/openid-connect/logout";
            if (idToken != null) {
                logoutUrl += "?id_token_hint=" + idToken;
            }
            logoutUrl += "&post_logout_redirect_uri=http://localhost:8081/";
            response.sendRedirect(logoutUrl);
        };
    }
}
