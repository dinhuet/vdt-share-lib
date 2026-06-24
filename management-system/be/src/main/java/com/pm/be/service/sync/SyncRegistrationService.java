package com.pm.be.service.sync;

import com.pm.be.entity.clientapi.ClientApiEntity;
import com.pm.be.entity.exposedapi.ExposedApiEntity;
import com.pm.be.entity.microservice.MicroServiceEntity;
import com.pm.be.enums.RegistrationSource;
import com.pm.be.enums.SyncStatus;
import com.pm.be.repository.clientapi.ClientApiRepository;
import com.pm.be.repository.exposedapi.ExposedApiRepository;
import com.pm.be.repository.microservice.MicroServiceRepository;
import com.pm.be.service.apidefaultconfig.ApiDefaultConfigResolver;
import com.pm.be.service.clientapi.ClientApiRedisSyncService;
import com.pm.be.service.exposedapi.ExposedApiRedisSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncRegistrationService {

    private final MicroServiceRepository microServiceRepo;
    private final ExposedApiRepository exposedApiRepo;
    private final ClientApiRepository clientApiRepo;
    private final ApiDefaultConfigResolver apiDefaultConfigResolver;
    private final ExposedApiRedisSyncService exposedApiRedisSyncService;
    private final ClientApiRedisSyncService clientApiRedisSyncService;

    public void sync(String serviceName, String serviceUrl,
                      java.util.List<ExposedApiInfo> exposedApis,
                      java.util.List<ClientApiInfo> clientApis) {

        if (!StringUtils.hasText(serviceName)) {
            throw new IllegalArgumentException("serviceName must not be blank");
        }

        var savedService = upsertMicroService(serviceName, serviceUrl);
        log.info("Upserted micro_service: {} (id={})", savedService.getName(), savedService.getId());

        var now = LocalDateTime.now();
        var existingApis = exposedApiRepo.findByMicroServiceId(savedService.getId());
        for (var existingApi : existingApis) {
                existingApi.setSyncStatus(SyncStatus.STALE);
                existingApi.setUpdatedAt(now);
        }
        exposedApiRepo.saveAllAndFlush(existingApis);
        exposedApiRedisSyncService.syncAll(existingApis);

        var existingClientApis = clientApiRepo.findByMicroServiceId(savedService.getId());
        for (var existingClientApi : existingClientApis) {
                existingClientApi.setSyncStatus(SyncStatus.STALE);
                existingClientApi.setUpdatedAt(now);
        }
        clientApiRepo.saveAllAndFlush(existingClientApis);
        clientApiRedisSyncService.syncAll(existingClientApis);

        if (exposedApis != null) {
            for (var api : exposedApis) {
                var existingEntity = exposedApiRepo.findByMicroServiceIdAndEndpointId(savedService.getId(), api.endpointId());
                var entity = existingEntity
                        .orElseGet(() -> {
                            var e = new ExposedApiEntity();
                            e.setMicroServiceId(savedService.getId());
                            e.setUseDefaultConfig(true);
                            e.setRegistrationSource(RegistrationSource.KAFKA_SYNC);
                            apiDefaultConfigResolver.applyTo(e);
                            e.setCreatedAt(now);
                            return e;
                        });
                applyExposedApi(entity, api, now);
                var savedApi = saveExposedApi(entity, savedService.getId(), api, now);
                exposedApiRedisSyncService.syncApi(savedApi);
                log.info("Upserted exposed_api: {}", api.name());
            }
        }

        if (clientApis != null) {
            for (var api : clientApis) {
                var existingEntity = clientApiRepo.findByMicroServiceIdAndEndpointId(savedService.getId(), api.endpointId());
                var entity = existingEntity
                        .orElseGet(() -> {
                            var e = new ClientApiEntity();
                            e.setMicroServiceId(savedService.getId());
                            e.setName(api.name());
                            e.setUseDefaultConfig(true);
                            apiDefaultConfigResolver.applyTo(e);
                            e.setCreatedAt(now);
                            return e;
                        });
                applyClientApi(entity, api, now);
                var savedClientApi = saveClientApi(entity, savedService.getId(), api, now);
                clientApiRedisSyncService.syncApi(savedClientApi);
                log.info("Upserted client_api: {}", api.name());
            }
        }
    }

    private MicroServiceEntity upsertMicroService(String serviceName, String serviceUrl) {
        var now = LocalDateTime.now();
        var service = microServiceRepo.findByName(serviceName)
                .orElseGet(() -> {
                    var svc = new MicroServiceEntity();
                    svc.setName(serviceName);
                    svc.setCreatedAt(now);
                    return svc;
                });
        applyMicroService(service, serviceName, serviceUrl, now);

        try {
            return microServiceRepo.saveAndFlush(service);
        } catch (DataIntegrityViolationException e) {
            var existing = microServiceRepo.findByName(serviceName)
                    .orElseThrow(() -> e);
            applyMicroService(existing, serviceName, serviceUrl, now);
            return microServiceRepo.saveAndFlush(existing);
        }
    }

    private void applyMicroService(MicroServiceEntity service, String serviceName, String serviceUrl, LocalDateTime now) {
        service.setName(serviceName);
        service.setServiceUrl(serviceUrl);
        service.setStatus("ACTIVE");
        service.setUpdatedAt(now);
    }

    private ExposedApiEntity saveExposedApi(ExposedApiEntity entity, UUID microServiceId, ExposedApiInfo api, LocalDateTime now) {
        try {
            return exposedApiRepo.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            var existing = exposedApiRepo.findByMicroServiceIdAndEndpointId(microServiceId, api.endpointId())
                    .orElseThrow(() -> e);
            applyExposedApi(existing, api, now);
            return exposedApiRepo.saveAndFlush(existing);
        }
    }

    private ClientApiEntity saveClientApi(ClientApiEntity entity, UUID microServiceId, ClientApiInfo api, LocalDateTime now) {
        try {
            return clientApiRepo.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            var existing = clientApiRepo.findByMicroServiceIdAndEndpointId(microServiceId, api.endpointId())
                    .orElseThrow(() -> e);
            applyClientApi(existing, api, now);
            return clientApiRepo.saveAndFlush(existing);
        }
    }

    private void applyExposedApi(ExposedApiEntity entity, ExposedApiInfo api, LocalDateTime now) {
        entity.setEndpointId(api.endpointId());
        entity.setEndpointKey(api.endpointKey());
        entity.setName(api.name());
        entity.setPath(api.path());
        entity.setTopic(api.topic());
        entity.setMethod(api.method());
        entity.setProtocol(api.protocol());
        entity.setSyncStatus(SyncStatus.ACTIVE);
        entity.setLastSyncedAt(now);
        entity.setUpdatedAt(now);
    }

    private void applyClientApi(ClientApiEntity entity, ClientApiInfo api, LocalDateTime now) {
        entity.setEndpointId(api.endpointId());
        entity.setEndpointKey(api.endpointKey());
        entity.setName(api.name());
        entity.setDestinationUrl(api.destinationUrl());
        entity.setTopic(api.topic());
        entity.setMethod(api.method());
        entity.setProtocol(api.protocol());
        entity.setSyncStatus(SyncStatus.ACTIVE);
        entity.setLastSyncedAt(now);
        entity.setUpdatedAt(now);
    }

    public record ExposedApiInfo(UUID endpointId, String endpointKey, String name, String path, String topic, String method, String protocol) {}
    public record ClientApiInfo(UUID endpointId, String endpointKey, String name, String destinationUrl, String topic, String method, String protocol) {}
}
