package com.menglang.ticket.application.service;

import com.menglang.ticket.domain.exception.ResourceNotFoundException;
import com.menglang.ticket.domain.exception.UnauthorizedClientException;
import com.menglang.ticket.domain.model.ApiClient;
import com.menglang.ticket.domain.port.in.ApiClientUseCase;
import com.menglang.ticket.domain.port.out.ApiClientRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiClientApplicationService implements ApiClientUseCase {

    private final ApiClientRepositoryPort apiClientRepositoryPort;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public Flux<ApiClient> listApiClients(Long companyId) {
        return apiClientRepositoryPort.findByCompanyId(companyId);
    }

    @Override
    public Mono<Map<String, Object>> createApiClient(Long companyId, ApiClient client) {
        client.setCompanyId(companyId != null ? companyId : 1L);
        if (client.getClientId() == null || client.getClientId().isBlank()) {
            client.setClientId(generateClientId());
        }

        String plainSecret = (client.getClientSecret() != null && !client.getClientSecret().isBlank())
                ? client.getClientSecret()
                : generateSecret();

        client.setClientSecret(plainSecret); // In production, hash via BCrypt / Argon2
        client.setIsActive(true);
        client.setCreatedAt(Instant.now());
        client.setUpdatedAt(Instant.now());

        return apiClientRepositoryPort.save(client)
                .map(saved -> Map.of(
                        "client", saved,
                        "plain_secret", plainSecret
                ));
    }

    @Override
    public Mono<ApiClient> getApiClient(String id, Long companyId) {
        return apiClientRepositoryPort.findById(id, companyId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("API client not found: " + id)));
    }

    @Override
    public Mono<Map<String, Object>> updateApiClient(String id, Long companyId, Map<String, Object> fields) {
        return getApiClient(id, companyId)
                .flatMap(client -> {
                    if (fields.containsKey("name") && fields.get("name") != null) {
                        client.setName(String.valueOf(fields.get("name")));
                    }
                    if (fields.containsKey("is_active") && fields.get("is_active") != null) {
                        client.setIsActive(Boolean.parseBoolean(String.valueOf(fields.get("is_active"))));
                    }

                    String newPlainSecret = null;
                    if (Boolean.TRUE.equals(fields.get("regenerate_secret"))) {
                        newPlainSecret = generateSecret();
                        client.setClientSecret(newPlainSecret);
                    } else if (fields.containsKey("client_secret") && fields.get("client_secret") != null) {
                        newPlainSecret = String.valueOf(fields.get("client_secret"));
                        client.setClientSecret(newPlainSecret);
                    }

                    client.setUpdatedAt(Instant.now());
                    String finalSecret = newPlainSecret;
                    return apiClientRepositoryPort.save(client)
                            .map(saved -> {
                                if (finalSecret != null) {
                                    return Map.of("client", saved, "plain_secret", finalSecret);
                                }
                                return Map.of("client", saved);
                            });
                });
    }

    @Override
    public Mono<Boolean> deleteApiClient(String id, Long companyId) {
        return apiClientRepositoryPort.delete(id, companyId);
    }

    @Override
    public Mono<ApiClient> toggleStatus(String id, Long companyId) {
        return getApiClient(id, companyId)
                .flatMap(client -> {
                    client.setIsActive(!Boolean.TRUE.equals(client.getIsActive()));
                    client.setUpdatedAt(Instant.now());
                    return apiClientRepositoryPort.save(client);
                });
    }

    @Override
    public Mono<Map<String, Object>> regenerateSecret(String id, Long companyId) {
        return getApiClient(id, companyId)
                .flatMap(client -> {
                    String newSecret = generateSecret();
                    client.setClientSecret(newSecret);
                    client.setUpdatedAt(Instant.now());
                    return apiClientRepositoryPort.save(client)
                            .map(saved -> Map.of(
                                    "client", saved,
                                    "plain_secret", newSecret
                            ));
                });
    }

    @Override
    public Mono<ApiClient> authenticate(String clientId, String clientSecret) {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            return Mono.error(new UnauthorizedClientException("Invalid API client credentials"));
        }

        String secretSha256 = sha256(clientSecret);

        return apiClientRepositoryPort.findByClientId(clientId)
                .filter(client -> Boolean.TRUE.equals(client.getIsActive()) &&
                        (clientSecret.equals(client.getClientSecret()) || secretSha256.equals(client.getClientSecret())))
                .switchIfEmpty(Mono.error(new UnauthorizedClientException("Invalid API client credentials")));
    }

    private String sha256(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            return "";
        }
    }

    private String generateClientId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 13).toUpperCase();
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
