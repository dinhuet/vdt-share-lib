package com.pm.be.service;

import com.pm.be.entity.ExposedApiEntity;
import com.pm.be.entity.MicroServiceEntity;
import com.pm.be.enums.SyncStatus;
import com.pm.be.repository.ClientApiRepository;
import com.pm.be.repository.ExposedApiRepository;
import com.pm.be.repository.MicroServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncRegistrationServiceTest {

    @Mock MicroServiceRepository microServiceRepo;
    @Mock ExposedApiRepository exposedApiRepo;
    @Mock ClientApiRepository clientApiRepo;
    @Mock ApiDefaultConfigResolver apiDefaultConfigResolver;
    @Mock ExposedApiRedisSyncService exposedApiRedisSyncService;
    @Mock ClientApiRedisSyncService clientApiRedisSyncService;

    private SyncRegistrationService syncService;

    @BeforeEach
    void setUp() {
        syncService = new SyncRegistrationService(
                microServiceRepo,
                exposedApiRepo,
                clientApiRepo,
                apiDefaultConfigResolver,
                exposedApiRedisSyncService,
                clientApiRedisSyncService);
    }

    @Test
    void sync_shouldUpdateExistingEndpointByMicroServiceIdAndEndpointId() {
        var serviceId = UUID.randomUUID();
        var endpointId = UUID.randomUUID();
        var service = microService(serviceId, "order-service");
        var existingApi = new ExposedApiEntity();
        existingApi.setId(UUID.randomUUID());
        existingApi.setMicroServiceId(serviceId);
        existingApi.setEndpointId(endpointId);
        existingApi.setName("old-name");

        when(microServiceRepo.findByName("order-service")).thenReturn(Optional.of(service));
        when(microServiceRepo.saveAndFlush(service)).thenReturn(service);
        when(exposedApiRepo.findByMicroServiceId(serviceId)).thenReturn(List.of(existingApi));
        when(clientApiRepo.findByMicroServiceId(serviceId)).thenReturn(List.of());
        when(exposedApiRepo.findByMicroServiceIdAndEndpointId(serviceId, endpointId)).thenReturn(Optional.of(existingApi));
        when(exposedApiRepo.saveAndFlush(existingApi)).thenReturn(existingApi);

        var api = new SyncRegistrationService.ExposedApiInfo(
                endpointId,
                "EXPOSED:HTTP:GET:/api/orders",
                "get-orders",
                "/api/orders",
                null,
                "GET",
                "HTTP");

        syncService.sync("order-service", "http://localhost:8082", List.of(api), List.of());

        assertThat(existingApi.getName()).isEqualTo("get-orders");
        assertThat(existingApi.getSyncStatus()).isEqualTo(SyncStatus.ACTIVE);
        verify(apiDefaultConfigResolver, never()).applyTo(any(ExposedApiEntity.class));
        verify(exposedApiRedisSyncService).syncApi(existingApi);
    }

    @Test
    void sync_shouldRetryEndpointUpdateWhenConcurrentInsertWins() {
        var serviceId = UUID.randomUUID();
        var endpointId = UUID.randomUUID();
        var service = microService(serviceId, "order-service");
        var concurrentApi = new ExposedApiEntity();
        concurrentApi.setId(UUID.randomUUID());
        concurrentApi.setMicroServiceId(serviceId);
        concurrentApi.setEndpointId(endpointId);

        when(microServiceRepo.findByName("order-service")).thenReturn(Optional.of(service));
        when(microServiceRepo.saveAndFlush(service)).thenReturn(service);
        when(exposedApiRepo.findByMicroServiceId(serviceId)).thenReturn(List.of());
        when(clientApiRepo.findByMicroServiceId(serviceId)).thenReturn(List.of());
        when(exposedApiRepo.findByMicroServiceIdAndEndpointId(serviceId, endpointId))
                .thenReturn(Optional.empty(), Optional.of(concurrentApi));
        when(exposedApiRepo.saveAndFlush(any(ExposedApiEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate endpoint"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var api = new SyncRegistrationService.ExposedApiInfo(
                endpointId,
                "EXPOSED:HTTP:POST:/api/orders",
                "create-order",
                "/api/orders",
                null,
                "POST",
                "HTTP");

        syncService.sync("order-service", "http://localhost:8082", List.of(api), List.of());

        assertThat(concurrentApi.getName()).isEqualTo("create-order");
        assertThat(concurrentApi.getEndpointKey()).isEqualTo("EXPOSED:HTTP:POST:/api/orders");
        assertThat(concurrentApi.getSyncStatus()).isEqualTo(SyncStatus.ACTIVE);
        verify(exposedApiRedisSyncService).syncApi(concurrentApi);
    }

    private MicroServiceEntity microService(UUID serviceId, String serviceName) {
        var service = new MicroServiceEntity();
        service.setId(serviceId);
        service.setName(serviceName);
        return service;
    }
}
