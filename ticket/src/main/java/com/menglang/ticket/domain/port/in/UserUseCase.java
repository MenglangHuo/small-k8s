package com.menglang.ticket.domain.port.in;

import com.menglang.ticket.domain.model.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface UserUseCase {
    Flux<User> listUsers(Long companyId, int page, int perPage, String search);
    Mono<Long> countUsers(Long companyId, String search);
    Flux<User> listAgents(Long companyId);
    Mono<User> getUserById(String id, Long companyId);
    Mono<User> getUserByUserId(Long userId, Long companyId);
    Mono<User> createUser(User user, String rawPassword);
    Mono<User> updateUser(String id, Long companyId, Map<String, Object> updates, String newPassword);
    Mono<User> toggleStatus(String id, Long companyId);
}
