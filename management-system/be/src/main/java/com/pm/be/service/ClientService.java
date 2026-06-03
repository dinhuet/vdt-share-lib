package com.pm.be.service;

import com.pm.be.dto.request.ClientCreateRequest;
import com.pm.be.dto.request.ClientRevokeRequest;
import com.pm.be.dto.request.ClientUpdateRequest;
import com.pm.be.dto.response.ClientResponse;
import com.pm.be.entity.ClientEntity;
import com.pm.be.enums.ClientStatus;
import com.pm.be.exception.AppException;
import com.pm.be.exception.ErrorCode;
import com.pm.be.repository.ClientRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final ClientRepository clientRepo;

    public List<ClientResponse> getAll(ClientStatus status, String search) {
        return clientRepo.findAll(buildSpec(status, search)).stream()
                .map(this::toResponse)
                .toList();
    }

    public ClientResponse getById(UUID id) {
        return toResponse(getEntity(id));
    }

    public ClientResponse create(ClientCreateRequest request) {
        validateRequest(request.getName(), request.getClientCode(), request.getEmail());
        String clientCode = request.getClientCode().trim();
        if (clientRepo.existsByClientCode(clientCode)) {
            throw new AppException(ErrorCode.CLIENT_EXISTED);
        }

        var now = LocalDateTime.now();
        var entity = ClientEntity.builder()
                .name(request.getName().trim())
                .clientCode(clientCode)
                .description(trimToNull(request.getDescription()))
                .email(trimToNull(request.getEmail()))
                .status(ClientStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return toResponse(clientRepo.save(entity));
    }

    public ClientResponse update(UUID id, ClientUpdateRequest request) {
        validateRequest(request.getName(), request.getClientCode(), request.getEmail());

        var entity = getEntity(id);
        if (entity.getStatus() == ClientStatus.REVOKED) {
            throw new AppException(ErrorCode.CLIENT_STATUS_INVALID);
        }
        String clientCode = request.getClientCode().trim();
        clientRepo.findByClientCode(clientCode)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new AppException(ErrorCode.CLIENT_EXISTED);
                });

        entity.setName(request.getName().trim());
        entity.setClientCode(clientCode);
        entity.setDescription(trimToNull(request.getDescription()));
        entity.setEmail(trimToNull(request.getEmail()));
        entity.setUpdatedAt(LocalDateTime.now());
        return toResponse(clientRepo.save(entity));
    }

    public ClientResponse activate(UUID id) {
        var entity = getEntity(id);
        if (entity.getStatus() == ClientStatus.REVOKED) {
            throw new AppException(ErrorCode.CLIENT_STATUS_INVALID);
        }
        entity.setStatus(ClientStatus.ACTIVE);
        entity.setUpdatedAt(LocalDateTime.now());
        return toResponse(clientRepo.save(entity));
    }

    public ClientResponse deactivate(UUID id) {
        var entity = getEntity(id);
        if (entity.getStatus() == ClientStatus.REVOKED) {
            throw new AppException(ErrorCode.CLIENT_STATUS_INVALID);
        }
        entity.setStatus(ClientStatus.INACTIVE);
        entity.setUpdatedAt(LocalDateTime.now());
        return toResponse(clientRepo.save(entity));
    }

    public ClientResponse revoke(UUID id, ClientRevokeRequest request) {
        if (request == null || !StringUtils.hasText(request.getReason())) {
            throw new AppException(ErrorCode.CLIENT_INVALID);
        }

        var entity = getEntity(id);
        if (entity.getStatus() == ClientStatus.REVOKED) {
            throw new AppException(ErrorCode.CLIENT_STATUS_INVALID);
        }

        var now = LocalDateTime.now();
        entity.setStatus(ClientStatus.REVOKED);
        entity.setRevokedAt(now);
        entity.setRevokedBy(resolveCurrentUser());
        entity.setRevokeReason(request.getReason().trim());
        entity.setUpdatedAt(now);
        return toResponse(clientRepo.save(entity));
    }

    public void delete(UUID id) {
        var entity = getEntity(id);
        if (entity.getStatus() != ClientStatus.REVOKED) {
            throw new AppException(ErrorCode.CLIENT_DELETE_NOT_ALLOWED);
        }
        clientRepo.delete(entity);
    }

    private ClientEntity getEntity(UUID id) {
        return clientRepo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CLIENT_NOTFOUND));
    }

    private Specification<ClientEntity> buildSpec(ClientStatus status, String search) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("clientCode")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern)
                ));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void validateRequest(String name, String clientCode, String email) {
        if (!StringUtils.hasText(name) || !StringUtils.hasText(clientCode)) {
            throw new AppException(ErrorCode.CLIENT_INVALID);
        }
        if (StringUtils.hasText(email) && !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new AppException(ErrorCode.CLIENT_INVALID);
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            return "SYSTEM";
        }
        return authentication.getName();
    }

    private ClientResponse toResponse(ClientEntity entity) {
        return ClientResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .clientCode(entity.getClientCode())
                .description(entity.getDescription())
                .email(entity.getEmail())
                .status(entity.getStatus())
                .revokedAt(entity.getRevokedAt())
                .revokedBy(entity.getRevokedBy())
                .revokeReason(entity.getRevokeReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
