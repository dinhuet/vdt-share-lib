package com.pm.sharedlib.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaxResponseSizeAdviceTest {

    MaxResponseSizeAdvice advice;

    @Mock
    HttpServletRequest servletRequest;
    @Mock
    ServerHttpResponse response;
    @Mock
    MethodParameter returnType;

    ExposedApiRuntimeConfig config;

    @BeforeEach
    void setUp() {
        var objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        advice = new MaxResponseSizeAdvice(objectMapper);
        config = ExposedApiRuntimeConfig.builder()
                .id(java.util.UUID.randomUUID())
                .build();
    }

    @Test
    void shouldPassThroughWhenMaxResponseKbIsNull() {
        config.setMaxResponseKb(null);
        mockConfigInRequest();

        var result = beforeBodyWrite("hello");

        assertThat(result).isEqualTo("hello");
    }

    @Test
    void shouldPassThroughWhenMaxResponseKbIsZero() {
        config.setMaxResponseKb(0);
        mockConfigInRequest();

        var result = beforeBodyWrite("hello");

        assertThat(result).isEqualTo("hello");
    }

    @Test
    void shouldPassThroughWhenConfigIsNotInRequest_attribute() {
        var result = advice.beforeBodyWrite(
                "hello", returnType, MediaType.APPLICATION_JSON,
                jsonConverter(), request(), response);

        assertThat(result).isEqualTo("hello");
    }

    @Test
    void shouldPassThroughWhenContentTypeIsNotJson() {
        config.setMaxResponseKb(10);

        var result = advice.beforeBodyWrite(
                "hello", returnType, MediaType.TEXT_PLAIN,
                jsonConverter(), request(), response);

        assertThat(result).isEqualTo("hello");
    }

    @Test
    void shouldPassThroughWhenBodyWithinLimit() {
        config.setMaxResponseKb(10);
        mockConfigInRequest();

        var result = beforeBodyWrite("small body");

        assertThat(result).isEqualTo("small body");
    }

    @Test
    void shouldThrowWhenExceedsMaxResponseKb() {
        config.setMaxResponseKb(1);
        mockConfigInRequest();

        assertThatThrownBy(() -> beforeBodyWrite("x".repeat(2000)))
                .isInstanceOf(RuntimeSecurityException.class)
                .satisfies(e -> {
                    var rse = (RuntimeSecurityException) e;
                    assertThat(rse.getStatusCode()).isEqualTo(413);
                    assertThat(rse.getErrorCode()).isEqualTo(RuntimeSecurityErrorCodes.RESPONSE_TOO_LARGE);
                });
    }

    @Test
    void shouldPassThroughWithNullBody() {
        config.setMaxResponseKb(10);
        mockConfigInRequest();

        var result = beforeBodyWrite(null);

        assertThat(result).isNull();
    }

    @Test
    void shouldHandleObjectBodyWithinLimit() {
        config.setMaxResponseKb(10);
        mockConfigInRequest();

        var body = new TestDto("hello", "world");
        var result = beforeBodyWrite(body);

        assertThat(result).isSameAs(body);
    }

    @Test
    void shouldThrowWhenLargeObjectBody() {
        config.setMaxResponseKb(1);
        mockConfigInRequest();

        var body = new TestDto("a".repeat(5000), "b".repeat(5000));

        assertThatThrownBy(() -> beforeBodyWrite(body))
                .isInstanceOf(RuntimeSecurityException.class);
    }

    @Test
    void supportsReturnsTrue() {
        assertThat(advice.supports(returnType, StringHttpMessageConverter.class)).isTrue();
    }

    void mockConfigInRequest() {
        when(servletRequest.getAttribute("exposedApiConfig")).thenReturn(config);
    }

    Object beforeBodyWrite(Object body) {
        return advice.beforeBodyWrite(body, returnType, MediaType.APPLICATION_JSON,
                jsonConverter(), request(), response);
    }

    @SuppressWarnings("unchecked")
    Class<? extends HttpMessageConverter<?>> jsonConverter() {
        return (Class<? extends HttpMessageConverter<?>>) (Class<?>) StringHttpMessageConverter.class;
    }

    ServletServerHttpRequest request() {
        return new ServletServerHttpRequest(servletRequest);
    }

    record TestDto(String field1, String field2) {
    }
}
