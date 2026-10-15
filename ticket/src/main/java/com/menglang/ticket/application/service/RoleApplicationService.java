package com.menglang.ticket.application.service;

import com.menglang.ticket.domain.exception.ResourceNotFoundException;
import com.menglang.ticket.domain.model.Role;
import com.menglang.ticket.domain.port.in.RoleUseCase;
import com.menglang.ticket.domain.port.out.RoleRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleApplicationService implements RoleUseCase {

    private final RoleRepositoryPort roleRepositoryPort;

    @Override
    public Flux<Role> listRoles(Long companyId) {
        return roleRepositoryPort.findByCompanyIdOrIsSystemTrue(companyId);
    }

    @Override
    public Mono<Role> getRoleById(String id) {
        return roleRepositoryPort.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Role not found with id: " + id)));
    }

    @Override
    public Mono<Role> createRole(Role role) {
        if (role.getCode() == null || role.getCode().isBlank()) {
            return Mono.error(new IllegalArgumentException("Role code is required"));
        }
        if (role.getName() == null || role.getName().isBlank()) {
            return Mono.error(new IllegalArgumentException("Role name is required"));
        }

        role.setCode(role.getCode().trim().toUpperCase());
        return roleRepositoryPort.findByCodeAndCompanyId(role.getCode(), role.getCompanyId())
                .flatMap(existing -> Mono.<Role>error(new IllegalArgumentException("Role already exists for code: " + existing.getCode())))
                .switchIfEmpty(Mono.defer(() -> {
                    if (role.getRoleId() == null) {
                        return roleRepositoryPort.count()
                                .flatMap(count -> {
                                    role.setRoleId(count + 1);
                                    return roleRepositoryPort.save(role);
                                });
                    }
                    return roleRepositoryPort.save(role);
                }));
    }

    @Override
    public Mono<Role> updateRole(String id, Map<String, Object> updates) {
        return getRoleById(id)
                .flatMap(role -> {
                    if (Boolean.TRUE.equals(role.getIsSystem())) {
                        return Mono.error(new IllegalArgumentException("Cannot modify system default roles"));
                    }
                    if (updates.containsKey("name") && updates.get("name") != null) {
                        role.setName((String) updates.get("name"));
                    }
                    if (updates.containsKey("description") && updates.get("description") != null) {
                        role.setDescription((String) updates.get("description"));
                    }
                    if (updates.containsKey("permissions") && updates.get("permissions") instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> perms = (List<String>) updates.get("permissions");
                        role.setPermissions(perms);
                    }
                    return roleRepositoryPort.save(role);
                });
    }

    @Override
    public Mono<Void> deleteRole(String id) {
        return getRoleById(id)
                .flatMap(role -> {
                    if (Boolean.TRUE.equals(role.getIsSystem())) {
                        return Mono.error(new IllegalArgumentException("Cannot delete system default roles"));
                    }
                    return roleRepositoryPort.deleteById(id);
                });
    }
}
