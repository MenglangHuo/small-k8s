package com.menglang.ticket.presentation.controller;

import com.menglang.ticket.domain.model.User;
import com.menglang.ticket.domain.port.in.UserUseCase;
import com.menglang.ticket.presentation.request.CreateUserRequest;
import com.menglang.ticket.presentation.request.UpdateUserRequest;
import com.menglang.ticket.presentation.response.ApiResponse;
import com.menglang.ticket.presentation.response.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserUseCase userUseCase;

    @GetMapping
    public Mono<ApiResponse<Map<String, Object>>> index(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20", name = "per_page") int perPage,
            @RequestParam(required = false) String search,
            ServerWebExchange exchange) {

        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);

        Mono<List<UserResponse>> contentMono = userUseCase.listUsers(companyId, page, perPage, search)
                .map(UserResponse::fromDomain)
                .collectList();

        Mono<Long> totalMono = userUseCase.countUsers(companyId, search);

        return Mono.zip(contentMono, totalMono)
                .map(tuple -> {
                    List<UserResponse> content = tuple.getT1();
                    Long total = tuple.getT2();
                    long totalPages = (total + perPage - 1) / perPage;

                    Map<String, Object> pagination = Map.of(
                            "page", page,
                            "per_page", perPage,
                            "total", total,
                            "total_pages", totalPages,
                            "has_next", page < totalPages,
                            "has_previous", page > 1
                    );

                    Map<String, Object> data = Map.of(
                            "content", content,
                            "pagination", pagination
                    );

                    return ApiResponse.success(data, "Retrieved successfully.");
                });
    }

    @GetMapping("/agents")
    public Mono<ApiResponse<List<UserResponse>>> listAgents(ServerWebExchange exchange) {
        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);

        return userUseCase.listAgents(companyId)
                .map(UserResponse::fromDomain)
                .collectList()
                .map(ApiResponse::success);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<UserResponse>> store(
            @Valid @RequestBody CreateUserRequest request,
            ServerWebExchange exchange) {

        Long companyId = request.getCompanyId() != null
                ? request.getCompanyId()
                : (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);

        User user = User.builder()
                .companyId(companyId)
                .username(request.getUsername())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .avatarUrl(request.getAvatarUrl())
                .roleIds(request.getRoleIds())
                .roleNames(request.getRoleNames())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        return userUseCase.createUser(user, request.getPassword())
                .map(created -> ApiResponse.success(UserResponse.fromDomain(created), "User created successfully."));
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<UserResponse>> show(
            @PathVariable String id,
            ServerWebExchange exchange) {

        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);

        return userUseCase.getUserById(id, companyId)
                .map(user -> ApiResponse.success(UserResponse.fromDomain(user)));
    }

    @PutMapping("/{id}")
    public Mono<ApiResponse<UserResponse>> update(
            @PathVariable String id,
            @RequestBody UpdateUserRequest request,
            ServerWebExchange exchange) {

        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);

        return userUseCase.updateUser(id, companyId, request.toMap(), request.getPassword())
                .map(updated -> ApiResponse.success(UserResponse.fromDomain(updated), "User updated successfully."));
    }

    @PatchMapping("/{id}/status")
    public Mono<ApiResponse<UserResponse>> toggleStatus(
            @PathVariable String id,
            ServerWebExchange exchange) {

        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);

        return userUseCase.toggleStatus(id, companyId)
                .map(toggled -> ApiResponse.success(UserResponse.fromDomain(toggled), "User status updated successfully."));
    }
}
