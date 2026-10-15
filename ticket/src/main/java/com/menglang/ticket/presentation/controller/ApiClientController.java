package com.menglang.ticket.presentation.controller;

import com.menglang.ticket.domain.model.ApiClient;
import com.menglang.ticket.domain.port.in.ApiClientUseCase;
import com.menglang.ticket.presentation.request.CreateApiClientRequest;
import com.menglang.ticket.presentation.request.UpdateApiClientRequest;
import com.menglang.ticket.presentation.response.ApiClientResponse;
import com.menglang.ticket.presentation.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping({"/api/v1/api-clients", "/api/v1/tickets/api-clients"})
@RequiredArgsConstructor
public class ApiClientController {

    private final ApiClientUseCase apiClientUseCase;

    @GetMapping
    public Mono<ApiResponse<List<ApiClientResponse>>> index(ServerWebExchange exchange) {
        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);

        return apiClientUseCase.listApiClients(companyId)
                .map(ApiClientResponse::fromDomain)
                .collectList()
                .map(ApiResponse::success);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<Map<String, Object>>> store(
            @Valid @RequestBody CreateApiClientRequest request,
            ServerWebExchange exchange) {

        Long companyId = request.getCompanyId() != null
                ? request.getCompanyId()
                : (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);

        ApiClient client = ApiClient.builder()
                .name(request.getName())
                .clientId(request.getClientId())
                .clientSecret(request.getClientSecret())
                .isActive(request.getIsActive())
                .companyId(companyId)
                .build();

        return apiClientUseCase.createApiClient(companyId, client)
                .map(map -> {
                    ApiClient created = (ApiClient) map.get("client");
                    String secret = (String) map.get("plain_secret");

                    Map<String, Object> data = new HashMap<>();
                    data.put("client", ApiClientResponse.fromDomain(created));
                    data.put("plain_secret", secret);
                    data.put("credentials_note", "Please copy and store this client_secret safely. For security reasons, it cannot be displayed again.");

                    return ApiResponse.success(data, "API client created successfully.");
                });
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<ApiClientResponse>> show(
            @PathVariable String id,
            ServerWebExchange exchange) {

        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);

        return apiClientUseCase.getApiClient(id, companyId)
                .map(client -> ApiResponse.success(ApiClientResponse.fromDomain(client)));
    }

    @PutMapping("/{id}")
    public Mono<ApiResponse<Map<String, Object>>> update(
            @PathVariable String id,
            @RequestBody UpdateApiClientRequest request,
            ServerWebExchange exchange) {

        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);

        return apiClientUseCase.updateApiClient(id, companyId, request.toMap())
                .map(map -> {
                    ApiClient updated = (ApiClient) map.get("client");
                    Map<String, Object> data = new HashMap<>();
                    data.put("client", ApiClientResponse.fromDomain(updated));
                    if (map.containsKey("plain_secret")) {
                        data.put("plain_secret", map.get("plain_secret"));
                        data.put("credentials_note", "Please copy and store this new client_secret safely. For security reasons, it cannot be displayed again.");
                    }
                    return ApiResponse.success(data, "API client updated successfully.");
                });
    }

    @DeleteMapping("/{id}")
    public Mono<ApiResponse<Void>> destroy(
            @PathVariable String id,
            ServerWebExchange exchange) {

        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);

        return apiClientUseCase.deleteApiClient(id, companyId)
                .map(deleted -> ApiResponse.success(null, "API client deleted successfully."));
    }

    @PatchMapping("/{id}/status")
    public Mono<ApiResponse<ApiClientResponse>> toggleStatus(
            @PathVariable String id,
            ServerWebExchange exchange) {

        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);

        return apiClientUseCase.toggleStatus(id, companyId)
                .map(client -> ApiResponse.success(ApiClientResponse.fromDomain(client), "API client status updated successfully."));
    }

    @PostMapping("/{id}/regenerate-secret")
    public Mono<ApiResponse<Map<String, Object>>> regenerateSecret(
            @PathVariable String id,
            ServerWebExchange exchange) {

        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);

        return apiClientUseCase.regenerateSecret(id, companyId)
                .map(map -> {
                    ApiClient client = (ApiClient) map.get("client");
                    String plainSecret = (String) map.get("plain_secret");

                    Map<String, Object> data = new HashMap<>();
                    data.put("client", ApiClientResponse.fromDomain(client));
                    data.put("plain_secret", plainSecret);
                    data.put("credentials_note", "Please copy and store this new client_secret safely. For security reasons, it cannot be displayed again.");

                    return ApiResponse.success(data, "API client secret rotated successfully.");
                });
    }
}
