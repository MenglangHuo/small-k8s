package com.menglang.ticket.infrastructure.persistence.adapter;

import com.menglang.ticket.domain.model.Role;
import com.menglang.ticket.domain.port.out.RoleRepositoryPort;
import com.menglang.ticket.infrastructure.persistence.document.RoleDocument;
import com.menglang.ticket.infrastructure.persistence.repository.SpringDataRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class MongoRoleRepositoryAdapter implements RoleRepositoryPort {

    private final SpringDataRoleRepository repository;

    @Override
    public Flux<Role> findByCompanyIdOrIsSystemTrue(Long companyId) {
        return repository.findByCompanyIdOrIsSystemTrue(companyId).map(this::toDomain);
    }

    @Override
    public Mono<Role> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Mono<Role> findByCodeAndCompanyId(String code, Long companyId) {
        return repository.findByCodeAndCompanyId(code, companyId).map(this::toDomain);
    }

    @Override
    public Mono<Role> save(Role role) {
        RoleDocument doc = toDocument(role);
        if (doc.getCreatedAt() == null) {
            doc.setCreatedAt(Instant.now());
        }
        doc.setUpdatedAt(Instant.now());
        return repository.save(doc).map(this::toDomain);
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return repository.deleteById(id);
    }

    @Override
    public Mono<Long> count() {
        return repository.count();
    }

    private Role toDomain(RoleDocument doc) {
        if (doc == null) return null;
        return Role.builder()
                .id(doc.getId())
                .roleId(doc.getRoleId())
                .companyId(doc.getCompanyId())
                .name(doc.getName())
                .code(doc.getCode())
                .description(doc.getDescription())
                .permissions(doc.getPermissions())
                .isSystem(doc.getIsSystem())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }

    private RoleDocument toDocument(Role domain) {
        if (domain == null) return null;
        return RoleDocument.builder()
                .id(domain.getId())
                .roleId(domain.getRoleId())
                .companyId(domain.getCompanyId())
                .name(domain.getName())
                .code(domain.getCode())
                .description(domain.getDescription())
                .permissions(domain.getPermissions())
                .isSystem(domain.getIsSystem())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
