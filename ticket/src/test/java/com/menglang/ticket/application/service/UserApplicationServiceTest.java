package com.menglang.ticket.application.service;

import com.menglang.ticket.domain.model.User;
import com.menglang.ticket.domain.port.out.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserApplicationServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    private UserApplicationService userApplicationService;

    @BeforeEach
    void setUp() {
        userApplicationService = new UserApplicationService(userRepositoryPort);
    }

    @Test
    @DisplayName("createUser hashes password and saves user")
    void testCreateUser() {
        User newUser = User.builder()
                .companyId(1L)
                .username("john_doe")
                .fullName("John Doe")
                .email("john@example.com")
                .build();

        when(userRepositoryPort.findByEmail("john@example.com")).thenReturn(Mono.empty());
        when(userRepositoryPort.count()).thenReturn(Mono.just(5L));
        when(userRepositoryPort.save(any(User.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        Mono<User> result = userApplicationService.createUser(newUser, "Secret123");

        StepVerifier.create(result)
                .assertNext(created -> {
                    assertEquals("john@example.com", created.getEmail());
                    assertEquals(6L, created.getUserId());
                    assertNotNull(created.getPasswordHash());
                    assertNotEquals("Secret123", created.getPasswordHash());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("createUser fails when email already exists")
    void testCreateUserDuplicateEmail() {
        User existing = User.builder().email("john@example.com").build();
        when(userRepositoryPort.findByEmail("john@example.com")).thenReturn(Mono.just(existing));

        User newUser = User.builder()
                .username("john")
                .fullName("John")
                .email("john@example.com")
                .build();

        Mono<User> result = userApplicationService.createUser(newUser, "pass");

        StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}
