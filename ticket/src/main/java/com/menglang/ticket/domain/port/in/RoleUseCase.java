package com.menglang.ticket.domain.port.in;

import com.menglang.ticket.domain.model.Role;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface RoleUseCase {
    Flux<Role> listRoles(Long companyId);
    Mono<Role> getRoleById(String id);
    Mono<Role> createRole(Role role);
    Mono<Role> updateRole(String id, Map<String, Object> updates);
    Mono<Void> deleteRole(String id);
}
