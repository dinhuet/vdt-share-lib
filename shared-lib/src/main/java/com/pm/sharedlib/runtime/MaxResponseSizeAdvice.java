package com.pm.sharedlib.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.nio.charset.StandardCharsets;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class MaxResponseSizeAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request, ServerHttpResponse response) {

        if (!MediaType.APPLICATION_JSON.isCompatibleWith(selectedContentType)) {
            return body;
        }

        var config = resolveConfig(request);
        if (config == null || config.getMaxResponseKb() == null || config.getMaxResponseKb() <= 0) {
            return body;
        }

        try {
            long maxBytes = config.getMaxResponseKb() * 1024L;
            long bodySize = estimateSize(body);
            if (bodySize > maxBytes) {
                log.warn("Response body size {} bytes exceeds max {}KB for endpoint {}",
                        bodySize, config.getMaxResponseKb(), config.getEndpointId());
                throw new RuntimeSecurityException(
                        413,
                        RuntimeSecurityErrorCodes.RESPONSE_TOO_LARGE,
                        "Response body exceeds maximum allowed size of " + config.getMaxResponseKb() + "KB");
            }
        } catch (RuntimeSecurityException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to check response size for endpoint {}", config.getEndpointId(), e);
        }
        return body;
    }

    private ExposedApiRuntimeConfig resolveConfig(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            return (ExposedApiRuntimeConfig) httpRequest.getAttribute("exposedApiConfig");
        }
        return null;
    }

    private long estimateSize(Object body) throws JsonProcessingException {
        if (body == null) return 0;
        if (body instanceof byte[]) return ((byte[]) body).length;
        if (body instanceof String) return ((String) body).getBytes(StandardCharsets.UTF_8).length;
        return objectMapper.writeValueAsBytes(body).length;
    }
}
