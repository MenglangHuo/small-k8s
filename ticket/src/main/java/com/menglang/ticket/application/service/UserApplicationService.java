package com.menglang.ticket.application.service;

import com.menglang.ticket.domain.exception.ResourceNotFoundException;
import com.menglang.ticket.domain.model.User;
import com.menglang.ticket.domain.port.in.UserUseCase;
import com.menglang.ticket.domain.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserApplicationService implements UserUseCase {

    private final UserRepositoryPort userRepositoryPort;

    @Override
    public Flux<User> listUsers(Long companyId, int page, int perPage, String search) {
        int skip = Math.max(0, (page - 1) * perPage);
        return userRepositoryPort.findByCompanyIdWithPagination(companyId, skip, perPage, search);
    }

    @Override
    public Mono<Long> countUsers(Long companyId, String search) {
        return userRepositoryPort.countByCompanyId(companyId, search);
    }

    @Override
    public Flux<User> listAgents(Long companyId) {
        return userRepositoryPort.findAgentsByCompanyId(companyId);
    }

    @Override
    public Mono<User> getUserById(String id, Long companyId) {
        return userRepositoryPort.findByIdAndCompanyId(id, companyId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found with id: " + id)));
    }

    @Override
    public Mono<User> getUserByUserId(Long userId, Long companyId) {
        return userRepositoryPort.findByUserIdAndCompanyId(userId, companyId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found with user_id: " + userId)));
    }

    @Override
    public Mono<User> createUser(User user, String rawPassword) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return Mono.error(new IllegalArgumentException("Email is required"));
        }
        if (user.getFullName() == null || user.getFullName().isBlank()) {
            return Mono.error(new IllegalArgumentException("Full name is required"));
        }

        user.setEmail(user.getEmail().trim().toLowerCase());
        if (rawPassword != null && !rawPassword.isBlank()) {
            user.setPasswordHash(hashPassword(rawPassword));
        }

        return userRepositoryPort.findByEmail(user.getEmail())
                .flatMap(existing -> Mono.<User>error(new IllegalArgumentException("Email already in use: " + existing.getEmail())))
                .switchIfEmpty(Mono.defer(() -> {
                    if (user.getUserId() == null) {
                        return userRepositoryPort.count()
                                .flatMap(count -> {
                                    user.setUserId(count + 1);
                                    return userRepositoryPort.save(user);
                                });
                    }
                    return userRepositoryPort.save(user);
                }));
    }

    @Override
    public Mono<User> updateUser(String id, Long companyId, Map<String, Object> updates, String newPassword) {
        return getUserById(id, companyId)
                .flatMap(user -> {
                    if (updates.containsKey("full_name") && updates.get("full_name") != null) {
                        user.setFullName((String) updates.get("full_name"));
                    }
                    if (updates.containsKey("phone") && updates.get("phone") != null) {
                        user.setPhone((String) updates.get("phone"));
                    }
                    if (updates.containsKey("avatar_url") && updates.get("avatar_url") != null) {
                        user.setAvatarUrl((String) updates.get("avatar_url"));
                    }
                    if (updates.containsKey("role_ids") && updates.get("role_ids") instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Long> roles = (List<Long>) updates.get("role_ids");
                        user.setRoleIds(roles);
                    }
                    if (updates.containsKey("role_names") && updates.get("role_names") instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> roleNames = (List<String>) updates.get("role_names");
                        user.setRoleNames(roleNames);
                    }
                    if (updates.containsKey("is_active") && updates.get("is_active") != null) {
                        user.setIsActive(Boolean.parseBoolean(String.valueOf(updates.get("is_active"))));
                    }
                    if (newPassword != null && !newPassword.isBlank()) {
                        user.setPasswordHash(hashPassword(newPassword));
                    }
                    return userRepositoryPort.save(user);
                });
    }

    @Override
    public Mono<User> toggleStatus(String id, Long companyId) {
        return getUserById(id, companyId)
                .flatMap(user -> {
                    user.setIsActive(!Boolean.TRUE.equals(user.getIsActive()));
                    return userRepositoryPort.save(user);
                });
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
