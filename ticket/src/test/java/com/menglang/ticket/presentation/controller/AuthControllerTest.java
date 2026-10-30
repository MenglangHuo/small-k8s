package com.menglang.ticket.presentation.controller;

import com.menglang.ticket.domain.model.User;
import com.menglang.ticket.domain.port.out.UserRepositoryPort;
import com.menglang.ticket.infrastructure.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    private TokenService tokenService;
    private AuthController authController;

    private final String defaultHash = "ff7bd97b1a7789ddd2775122fd6817f3173672da9f802ceec57f284325bf589f";

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        authController = new AuthController(userRepositoryPort, tokenService);
        ReflectionTestUtils.setField(authController, "configuredApiSecret", "test-api-secret");
    }

    @Test
    @DisplayName("generateToken succeeds with valid API secret")
    void testGenerateTokenValidSecret() {
        Mono<ResponseEntity<Map<String, Object>>> result = authController.generateTokenDefault("test-api-secret");

        StepVerifier.create(result)
                .assertNext(res -> {
                    assertEquals(HttpStatus.OK, res.getStatusCode());
                    assertTrue((Boolean) res.getBody().get("success"));
                    assertNotNull(res.getBody().get("access_token"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("generateToken rejects with invalid API secret")
    void testGenerateTokenInvalidSecret() {
        Mono<ResponseEntity<Map<String, Object>>> result = authController.generateTokenDefault("wrong_secret");

        StepVerifier.create(result)
                .assertNext(res -> {
                    assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
                    assertFalse((Boolean) res.getBody().get("success"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("login succeeds with valid username and password")
    void testLoginSuccess() {
        User user = User.builder()
                .userId(1L)
                .username("admin")
                .email("admin@uyfc.org")
                .fullName("System Administrator")
                .passwordHash(defaultHash)
                .roleNames(List.of("ADMIN"))
                .isActive(true)
                .build();

        when(userRepositoryPort.findByUsername("admin")).thenReturn(Mono.just(user));

        Mono<ResponseEntity<Map<String, Object>>> result = authController.loginJson(Map.of(
                "username", "admin",
                "password", "Password@123"
        ));

        StepVerifier.create(result)
                .assertNext(res -> {
                    assertEquals(HttpStatus.OK, res.getStatusCode());
                    assertTrue((Boolean) res.getBody().get("success"));
                    assertNotNull(res.getBody().get("access_token"));
                    assertEquals("ADMIN", res.getBody().get("role"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("login rejects when password is incorrect")
    void testLoginWrongPassword() {
        User user = User.builder()
                .userId(1L)
                .username("admin")
                .email("admin@uyfc.org")
                .fullName("System Administrator")
                .passwordHash(defaultHash)
                .roleNames(List.of("ADMIN"))
                .isActive(true)
                .build();

        when(userRepositoryPort.findByUsername("admin")).thenReturn(Mono.just(user));

        Mono<ResponseEntity<Map<String, Object>>> result = authController.loginJson(Map.of(
                "username", "admin",
                "password", "wrong_password_123"
        ));

        StepVerifier.create(result)
                .assertNext(res -> {
                    assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
                    assertFalse((Boolean) res.getBody().get("success"));
                    assertEquals("Invalid username or password", res.getBody().get("message"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("login rejects when user does not exist in database")
    void testLoginUserNotFound() {
        when(userRepositoryPort.findByUsername("ghost")).thenReturn(Mono.empty());
        when(userRepositoryPort.findByEmail("ghost")).thenReturn(Mono.empty());

        Mono<ResponseEntity<Map<String, Object>>> result = authController.loginJson(Map.of(
                "username", "ghost",
                "password", "Password@123"
        ));

        StepVerifier.create(result)
                .assertNext(res -> {
                    assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
                    assertFalse((Boolean) res.getBody().get("success"));
                    assertEquals("Invalid username or password", res.getBody().get("message"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("login rejects when user account is deactivated")
    void testLoginInactiveUser() {
        User inactiveUser = User.builder()
                .userId(3L)
                .username("disabled_user")
                .email("disabled@uyfc.org")
                .passwordHash(defaultHash)
                .roleNames(List.of("STAFF"))
                .isActive(false)
                .build();

        when(userRepositoryPort.findByUsername("disabled_user")).thenReturn(Mono.just(inactiveUser));

        Mono<ResponseEntity<Map<String, Object>>> result = authController.loginJson(Map.of(
                "username", "disabled_user",
                "password", "Password@123"
        ));

        StepVerifier.create(result)
                .assertNext(res -> {
                    assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
                    assertFalse((Boolean) res.getBody().get("success"));
                    assertTrue(res.getBody().get("message").toString().contains("disabled"));
                })
                .verifyComplete();
    }
}
