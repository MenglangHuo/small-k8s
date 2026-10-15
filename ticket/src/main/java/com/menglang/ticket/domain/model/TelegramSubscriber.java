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
public class TelegramSubscriber {
    private String id;
    private Long companyId;
    private String telegramChatId;
    private String telegramUsername;
    private String inviteToken;
    
    @Builder.Default
    private Boolean isActive = true;
    
    @Builder.Default
    private Instant subscribedAt = Instant.now();
    
    private Instant createdAt;
    private Instant updatedAt;
}
