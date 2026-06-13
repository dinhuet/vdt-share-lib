package com.pm.sharedlib.endpoint;

import com.pm.sharedlib.annotation.ClientCall;
import com.pm.sharedlib.annotation.SharedApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Slf4j
public class EndpointScanner {

    private final ListableBeanFactory beanFactory;
    private final ObjectProvider<RequestMappingHandlerMapping> requestMappingHandlerMapping;

    public ScannedEndpoints scan() {
        var exposedApis = new ArrayList<>(scanExposedHttpApis());
        exposedApis.addAll(scanExposedMqApis());
        return new ScannedEndpoints(exposedApis, scanClientApis());
    }

    private List<EndpointDefinition> scanExposedHttpApis() {
        var mapping = requestMappingHandlerMapping.getIfAvailable();
        if (mapping == null) {
            log.debug("Skip exposed HTTP endpoint scan because RequestMappingHandlerMapping is not available");
            return List.of();
        }

        var endpoints = new ArrayList<EndpointDefinition>();
        for (var entry : mapping.getHandlerMethods().entrySet()) {
            var handlerMethod = entry.getValue();
            var sharedApi = handlerMethod.getMethodAnnotation(SharedApi.class);
            if (sharedApi == null) {
                continue;
            }

            for (var path : extractPaths(entry.getKey())) {
                for (var method : extractMethods(entry.getKey(), sharedApi)) {
                    endpoints.add(EndpointDefinition.builder()
                            .type(EndpointType.EXPOSED)
                            .protocol(sharedApi.protocol())
                            .name(sharedApi.name())
                            .method(method)
                            .path(path)
                            .handlerClass(handlerMethod.getBeanType().getName())
                            .handlerMethod(handlerMethod.getMethod().getName())
                            .build());
                }
            }
        }
        return endpoints;
    }

    private List<EndpointDefinition> scanExposedMqApis() {
        var endpoints = new ArrayList<EndpointDefinition>();
        for (var beanName : beanFactory.getBeanDefinitionNames()) {
            var bean = beanFactory.getBean(beanName);
            var targetClass = ClassUtils.getUserClass(bean);
            for (var method : targetClass.getMethods()) {
                var sharedApi = method.getAnnotation(SharedApi.class);
                if (sharedApi == null || !"MQ".equalsIgnoreCase(sharedApi.protocol())) {
                    continue;
                }
                var topic = resolveMqTopic(sharedApi, method);
                endpoints.add(EndpointDefinition.builder()
                        .type(EndpointType.EXPOSED)
                        .protocol("MQ")
                        .name(sharedApi.name())
                        .topic(topic)
                        .handlerClass(targetClass.getName())
                        .handlerMethod(method.getName())
                        .build());
            }
        }
        return endpoints;
    }

    private String resolveMqTopic(SharedApi sharedApi, Method method) {
        if (!sharedApi.topic().isBlank()) {
            return sharedApi.topic();
        }
        var kafkaListener = method.getAnnotation(KafkaListener.class);
        if (kafkaListener != null && kafkaListener.topics().length > 0) {
            return kafkaListener.topics()[0];
        }
        log.warn("MQ exposed API [{}] has no topic defined via @SharedApi.topic or @KafkaListener.topics", sharedApi.name());
        return "";
    }

    private List<EndpointDefinition> scanClientApis() {
        var endpoints = new ArrayList<EndpointDefinition>();
        for (var beanName : beanFactory.getBeanDefinitionNames()) {
            var bean = beanFactory.getBean(beanName);
            var targetClass = ClassUtils.getUserClass(bean);
            for (var method : targetClass.getMethods()) {
                var clientCall = method.getAnnotation(ClientCall.class);
                if (clientCall == null) {
                    continue;
                }
                endpoints.add(EndpointDefinition.builder()
                        .type(EndpointType.CLIENT)
                        .protocol(clientCall.protocol())
                        .name(clientCall.name())
                        .method(clientCall.method())
                        .destinationUrl(clientCall.destinationUrl())
                        .handlerClass(targetClass.getName())
                        .handlerMethod(method.getName())
                        .build());
            }
        }
        return endpoints;
    }

    private Collection<String> extractPaths(RequestMappingInfo info) {
        var pathPatterns = info.getPathPatternsCondition();
        if (pathPatterns != null && !pathPatterns.getPatternValues().isEmpty()) {
            return pathPatterns.getPatternValues();
        }
        return List.of();
    }

    private Collection<String> extractMethods(RequestMappingInfo info, SharedApi sharedApi) {
        Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
        if (!methods.isEmpty()) {
            return methods.stream().map(RequestMethod::name).toList();
        }
        if (sharedApi.method() != null && !sharedApi.method().isBlank()) {
            return List.of(sharedApi.method().toUpperCase());
        }
        return List.of("");
    }

    public record ScannedEndpoints(List<EndpointDefinition> exposedApis, List<EndpointDefinition> clientApis) {
    }
}
