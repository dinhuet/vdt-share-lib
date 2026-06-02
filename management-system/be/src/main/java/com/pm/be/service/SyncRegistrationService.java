package com.pm.be.service;

import com.pm.be.entity.ClientApiEntity;
import com.pm.be.entity.ExposedApiEntity;
import com.pm.be.entity.MicroServiceEntity;
import com.pm.be.enums.RegistrationSource;
import com.pm.be.enums.SyncStatus;
import com.pm.be.repository.ClientApiRepository;
import com.pm.be.repository.ExposedApiRepository;
import com.pm.be.repository.MicroServiceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SyncRegistrationService {

    private final MicroServiceRepository microServiceRepo;
    private final ExposedApiRepository exposedApiRepo;
    private final ClientApiRepository clientApiRepo;
    private final ApiDefaultConfigResolver apiDefaultConfigResolver;
    private final ExposedApiRedisSyncService exposedApiRedisSyncService;

    public void sync(String serviceName, String serviceUrl,
                      java.util.List<ExposedApiInfo> exposedApis,
                      java.util.List<ClientApiInfo> clientApis) {

        if (!StringUtils.hasText(serviceName)) {
            throw new IllegalArgumentException("serviceName must not be blank");
        }

        var service = microServiceRepo.findByName(serviceName)
                .orElseGet(() -> {
                    var svc = new MicroServiceEntity();
                    svc.setName(serviceName);
                    svc.setCreatedAt(LocalDateTime.now());
                    return svc;
                });

        service.setName(serviceName);
        service.setServiceUrl(serviceUrl);
        service.setStatus("ACTIVE");
        service.setUpdatedAt(LocalDateTime.now());
        var savedService = microServiceRepo.save(service);
        log.info("Upserted micro_service: {} (id={})", savedService.getName(), savedService.getId());

        var now = LocalDateTime.now();
        var existingApis = exposedApiRepo.findByMicroServiceId(savedService.getId());
        for (var existingApi : existingApis) {
            existingApi.setSyncStatus(SyncStatus.STALE);
            existingApi.setUpdatedAt(now);
        }
        exposedApiRepo.saveAll(existingApis);
        exposedApiRedisSyncService.syncAll(existingApis);

        if (exposedApis != null) {
            for (var api : exposedApis) {
                var existingEntity = exposedApiRepo.findByMicroServiceIdAndName(savedService.getId(), api.name());
                var entity = existingEntity
                        .orElseGet(() -> {
                            var e = new ExposedApiEntity();
                            e.setMicroServiceId(savedService.getId());
                            e.setName(api.name());
                            e.setUseDefaultConfig(true);
                            e.setRegistrationSource(RegistrationSource.KAFKA_SYNC);
                            apiDefaultConfigResolver.applyTo(e);
                            e.setCreatedAt(now);
                            return e;
                        });
                entity.setPath(api.path());
                entity.setMethod(api.method());
                entity.setProtocol(api.protocol());
                entity.setSyncStatus(SyncStatus.ACTIVE);
                entity.setLastSyncedAt(now);
                entity.setUpdatedAt(now);
                var savedApi = exposedApiRepo.save(entity);
                exposedApiRedisSyncService.syncApi(savedApi);
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
}
