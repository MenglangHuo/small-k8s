package com.menglang.ticket.infrastructure.security;

import com.menglang.ticket.domain.model.User;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to manage lifecycle, issuance, and verification of application tokens
 * and authenticated user session tokens.
 */
@Service
public class TokenService {

    public record SessionInfo(
            Long userId,
            String username,
            String email,
            Long companyId,
            String role,
            Instant expiresAt
    ) {
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private final Map<String, SessionInfo> sessionTokens = new ConcurrentHashMap<>();
    private final Map<String, Instant> appTokens = new ConcurrentHashMap<>();

    /**
     * Create an application-level token (from /token using secret).
     * Valid for 24 hours.
     */
    public String createAppToken() {
        String token = "tck_" + UUID.randomUUID().toString().replace("-", "");
        appTokens.put(token, Instant.now().plusSeconds(86400));
        return token;
    }

    /**
     * Validate an application-level token.
     */
    public boolean validateAppToken(String token) {
        if (token == null || token.isBlank()) return false;
        Instant expiry = appTokens.get(token);
        if (expiry == null || Instant.now().isAfter(expiry)) {
            if (expiry != null) appTokens.remove(token);
            return false;
        }
        return true;
    }

    /**
     * Create an authenticated user session token after valid credential check.
     * Valid for 7 days.
     */
    public String createSessionToken(User user) {
        String token = "usr_" + UUID.randomUUID().toString().replace("-", "");
        String role = (user.getRoleNames() != null && !user.getRoleNames().isEmpty())
                ? user.getRoleNames().get(0)
                : "STAFF";

        SessionInfo session = new SessionInfo(
                user.getUserId() != null ? user.getUserId() : 1L,
                user.getUsername(),
                user.getEmail(),
                user.getCompanyId() != null ? user.getCompanyId() : 1L,
                role,
                Instant.now().plusSeconds(86400 * 7)
        );
        sessionTokens.put(token, session);
        return token;
    }

    /**
     * Validate a user session token. Returns null if invalid or expired.
     */
    public SessionInfo validateSessionToken(String token) {
        if (token == null || token.isBlank()) return null;
        SessionInfo session = sessionTokens.get(token);
        if (session == null || session.isExpired()) {
            if (session != null) sessionTokens.remove(token);
            return null;
        }
        return session;
    }

    /**
     * Revoke a user or application token on logout.
     */
    public void revokeToken(String token) {
        if (token != null) {
            sessionTokens.remove(token);
            appTokens.remove(token);
        }
    }
}
