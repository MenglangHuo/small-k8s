package com.menglang.ticket.infrastructure.init;

import com.menglang.ticket.domain.model.ApiClient;
import com.menglang.ticket.domain.model.Company;
import com.menglang.ticket.domain.model.Role;
import com.menglang.ticket.domain.model.User;
import com.menglang.ticket.domain.port.out.ApiClientRepositoryPort;
import com.menglang.ticket.domain.port.out.CompanyRepositoryPort;
import com.menglang.ticket.domain.port.out.RoleRepositoryPort;
import com.menglang.ticket.domain.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CompanyRepositoryPort companyRepositoryPort;
    private final RoleRepositoryPort roleRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final ApiClientRepositoryPort apiClientRepositoryPort;

    @Override
    public void run(String... args) {
        initCompany()
                .then(initRoles())
                .then(initUsers())
                .then(initApiClient())
                .subscribe(
                        null,
                        error -> log.error("Failed to execute initial data seeding: {}", error.getMessage()),
                        () -> log.info("Data initialization check completed successfully.")
                );
    }

    private Mono<Void> initCompany() {
        return companyRepositoryPort.count()
                .filter(count -> count == 0)
                .flatMap(count -> {
                    Company defaultCompany = Company.builder()
                            .companyId(1L)
                            .name("Union of Youth Federations of Cambodia (UYFC)")
                            .code("UYFC")
                            .domain("uyfc.org")
                            .isActive(true)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();

                    log.info("Seeding default company: UYFC (company_id: 1)");
                    return companyRepositoryPort.save(defaultCompany);
                })
                .then();
    }

    private Mono<Void> initRoles() {
        return roleRepositoryPort.count()
                .filter(count -> count == 0)
                .flatMap(count -> {
                    Role adminRole = Role.builder()
                            .roleId(1L)
                            .companyId(0L)
                            .name("Administrator")
                            .code("ADMIN")
                            .description("Full administrative access across all system resources")
                            .permissions(List.of("ticket:all", "comment:all", "user:all", "role:all", "client:all", "report:all"))
                            .isSystem(true)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();

                    Role agentRole = Role.builder()
                            .roleId(2L)
                            .companyId(0L)
                            .name("Support Agent")
                            .code("AGENT")
                            .description("Can view, assign, respond to, and resolve assigned tickets")
                            .permissions(List.of("ticket:read", "ticket:create", "ticket:update", "ticket:assign", "comment:create", "comment:read"))
                            .isSystem(true)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();

                    Role customerRole = Role.builder()
                            .roleId(3L)
                            .companyId(0L)
                            .name("Customer")
                            .code("CUSTOMER")
                            .description("Can file tickets and reply to customer-facing comments")
                            .permissions(List.of("ticket:read", "ticket:create", "comment:create"))
                            .isSystem(true)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();

                    log.info("Seeding default system roles: ADMIN, AGENT, CUSTOMER");
                    return roleRepositoryPort.save(adminRole)
                            .then(roleRepositoryPort.save(agentRole))
                            .then(roleRepositoryPort.save(customerRole));
                })
                .then();
    }

    private Mono<Void> initUsers() {
        return userRepositoryPort.count()
                .filter(count -> count == 0)
                .flatMap(count -> {
                    String passwordHash = sha256("Password@123");

                    User admin = User.builder()
                            .userId(1L)
                            .companyId(1L)
                            .username("admin")
                            .fullName("System Administrator")
                            .email("admin@uyfc.org")
                            .phone("+85512345678")
                            .passwordHash(passwordHash)
                            .roleIds(List.of(1L))
                            .roleNames(List.of("ADMIN"))
                            .isActive(true)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();

                    User agent = User.builder()
                            .userId(2L)
                            .companyId(1L)
                            .username("agent")
                            .fullName("Support Agent")
                            .email("agent@uyfc.org")
                            .phone("+85598765432")
                            .passwordHash(passwordHash)
                            .roleIds(List.of(2L))
                            .roleNames(List.of("AGENT"))
                            .isActive(true)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();

                    log.info("Seeding default users: admin@uyfc.org and agent@uyfc.org (default password: Password@123)");
                    return userRepositoryPort.save(admin)
                            .then(userRepositoryPort.save(agent));
                })
                .then();
    }

    private Mono<Void> initApiClient() {
        return apiClientRepositoryPort.count()
                .filter(count -> count == 0)
                .flatMap(count -> {
                    String secret = "secret_demo_uyfc_12345";
                    String secretHash = sha256(secret);

                    ApiClient client = ApiClient.builder()
                            .companyId(1L)
                            .clientId("client_demo_uyfc")
                            .clientSecret(secretHash)
                            .name("Demo UYFC Portal Client")
                            .isActive(true)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();

                    log.info("Seeding default demo API Client: client_demo_uyfc");
                    return apiClientRepositoryPort.save(client);
                })
                .then();
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
