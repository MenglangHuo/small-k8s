package com.menglang.ticket.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelegramInvite {
    private String id;
    private Long companyId;
    private String token;
    
    @Builder.Default
    private Boolean isUsed = false;
    
    private Instant expiresAt;
    private Long createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    public boolean isValid() {
        return !Boolean.TRUE.equals(isUsed) && expiresAt != null && expiresAt.isAfter(Instant.now());
    }
}
