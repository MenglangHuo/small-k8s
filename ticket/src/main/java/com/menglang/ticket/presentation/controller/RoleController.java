package com.menglang.ticket.presentation.controller;

import com.menglang.ticket.domain.model.Role;
import com.menglang.ticket.domain.port.in.RoleUseCase;
import com.menglang.ticket.presentation.request.CreateRoleRequest;
import com.menglang.ticket.presentation.request.UpdateRoleRequest;
import com.menglang.ticket.presentation.response.ApiResponse;
import com.menglang.ticket.presentation.response.RoleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleUseCase roleUseCase;

    @GetMapping
    public Mono<ApiResponse<List<RoleResponse>>> index(ServerWebExchange exchange) {
        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);

        return roleUseCase.listRoles(companyId)
                .map(RoleResponse::fromDomain)
                .collectList()
                .map(ApiResponse::success);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<RoleResponse>> store(
            @Valid @RequestBody CreateRoleRequest request,
            ServerWebExchange exchange) {

        Long companyId = request.getCompanyId() != null
                ? request.getCompanyId()
                : (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);

        Role role = Role.builder()
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .companyId(companyId)
                .permissions(request.getPermissions())
                .isSystem(false)
                .build();

        return roleUseCase.createRole(role)
                .map(created -> ApiResponse.success(RoleResponse.fromDomain(created), "Role created successfully."));
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<RoleResponse>> show(@PathVariable String id) {
        return roleUseCase.getRoleById(id)
                .map(role -> ApiResponse.success(RoleResponse.fromDomain(role)));
    }

    @PutMapping("/{id}")
    public Mono<ApiResponse<RoleResponse>> update(
            @PathVariable String id,
            @RequestBody UpdateRoleRequest request) {
        return roleUseCase.updateRole(id, request.toMap())
                .map(updated -> ApiResponse.success(RoleResponse.fromDomain(updated), "Role updated successfully."));
    }

    @DeleteMapping("/{id}")
    public Mono<ApiResponse<Void>> destroy(@PathVariable String id) {
        return roleUseCase.deleteRole(id)
                .thenReturn(ApiResponse.success(null, "Role deleted successfully."));
    }
}
