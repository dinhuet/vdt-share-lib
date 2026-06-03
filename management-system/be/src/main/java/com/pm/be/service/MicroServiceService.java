package com.pm.be.service;

import com.pm.be.dto.response.MicroServiceResponse;
import com.pm.be.entity.MicroServiceEntity;
import com.pm.be.exception.AppException;
import com.pm.be.exception.ErrorCode;
import com.pm.be.repository.MicroServiceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MicroServiceService {
    private final MicroServiceRepository microServiceRepo;

    public List<MicroServiceResponse> getAll() {
        return microServiceRepo.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public MicroServiceResponse getById(UUID id) {
        return microServiceRepo.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new AppException(ErrorCode.MICROSERVICE_NOTFOUND));
    }

    private MicroServiceResponse toResponse(MicroServiceEntity entity) {
        return MicroServiceResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .serviceUrl(entity.getServiceUrl())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
