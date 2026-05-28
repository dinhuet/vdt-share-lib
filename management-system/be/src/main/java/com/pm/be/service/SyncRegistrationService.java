package com.pm.be.service;

import com.pm.be.config.ExposedApiDefaultProperties;
import com.pm.be.entity.ClientApiEntity;
import com.pm.be.entity.ExposedApiEntity;
import com.pm.be.entity.MicroServiceEntity;
import com.pm.be.repository.ClientApiRepository;
import com.pm.be.repository.ExposedApiRepository;
import com.pm.be.repository.MicroServiceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SyncRegistrationService {

    private final MicroServiceRepository microServiceRepo;
    private final ExposedApiRepository exposedApiRepo;
    private final ClientApiRepository clientApiRepo;
    private final ExposedApiDefaultProperties exposedApiDefaults;

    public void sync(String serviceName, String serviceUrl, String keyService,
                     java.util.List<ExposedApiInfo> exposedApis,
                     java.util.List<ClientApiInfo> clientApis) {

        var service = microServiceRepo.findByKeyService(keyService)
                .orElseGet(() -> {
                    var svc = new MicroServiceEntity();
                    svc.setKeyService(keyService);
                    svc.setCreatedAt(LocalDateTime.now());
                    return svc;
                });

        service.setName(serviceName);
        service.setServiceUrl(serviceUrl);
        service.setStatus("ACTIVE");
        service.setUpdatedAt(LocalDateTime.now());
        var savedService = microServiceRepo.save(service);
        log.info("Upserted micro_service: {} (id={})", savedService.getName(), savedService.getId());

        if (exposedApis != null) {
            for (var api : exposedApis) {
                var existingEntity = exposedApiRepo.findByMicroServiceIdAndName(savedService.getId(), api.name());
                var isNewEntity = existingEntity.isEmpty();
                var entity = existingEntity
                        .orElseGet(() -> {
                            var e = new ExposedApiEntity();
                            e.setMicroServiceId(savedService.getId());
                            e.setName(api.name());
                            e.setCreatedAt(LocalDateTime.now());
                            return e;
                        });
                entity.setPath(api.path());
                entity.setMethod(api.method());
                entity.setProtocol(api.protocol());
                applyExposedApiDefaults(entity, isNewEntity);
                entity.setUpdatedAt(LocalDateTime.now());
                exposedApiRepo.save(entity);
                log.info("Upserted exposed_api: {}", api.name());
            }
        }

        if (clientApis != null) {
            for (var api : clientApis) {
                var entity = clientApiRepo
                        .findByMicroServiceIdAndName(savedService.getId(), api.name())
                        .orElseGet(() -> {
                            var e = new ClientApiEntity();
                            e.setMicroServiceId(savedService.getId());
                            e.setName(api.name());
                            e.setCreatedAt(LocalDateTime.now());
                            return e;
                        });
                entity.setDestinationUrl(api.destinationUrl());
                entity.setMethod(api.method());
                entity.setProtocol(api.protocol());
                entity.setEnabled(true);
                entity.setUpdatedAt(LocalDateTime.now());
                clientApiRepo.save(entity);
                log.info("Upserted client_api: {}", api.name());
            }
        }
    }

    public record ExposedApiInfo(String name, String path, String method, String protocol) {}
    public record ClientApiInfo(String name, String destinationUrl, String method, String protocol) {}

    private void applyExposedApiDefaults(ExposedApiEntity entity, boolean force) {
        if (force || entity.getMaxRequests() == null) {
            entity.setMaxRequests(exposedApiDefaults.getMaxRequests());
        }
        if (force || entity.getThrottleWindowSec() == null) {
            entity.setThrottleWindowSec(exposedApiDefaults.getThrottleWindowSec());
        }
        if (force || entity.getMaxRequestKb() == null) {
            entity.setMaxRequestKb(exposedApiDefaults.getMaxRequestKb());
        }
        if (force || entity.getMaxResponseKb() == null) {
            entity.setMaxResponseKb(exposedApiDefaults.getMaxResponseKb());
        }
        if (force || entity.getLatencyThresholdMs() == null) {
            entity.setLatencyThresholdMs(exposedApiDefaults.getLatencyThresholdMs());
        }
        if (force || entity.getTimeoutMs() == null) {
            entity.setTimeoutMs(exposedApiDefaults.getTimeoutMs());
        }
        if (force || entity.getLogRetentionDays() == null) {
            entity.setLogRetentionDays(exposedApiDefaults.getLogRetentionDays());
        }
        if (force || entity.getEnabled() == null) {
            entity.setEnabled(exposedApiDefaults.getEnabled());
        }
    }
}
