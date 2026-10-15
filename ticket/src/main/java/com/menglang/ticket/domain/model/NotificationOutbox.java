package com.menglang.ticket.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationOutbox {
    private String id;
    private String ticketId;
    private Long companyId;
    
    @Builder.Default
    private String channel = "telegram";
    
    private String eventType;
    
    @Builder.Default
    private Map<String, Object> payload = new HashMap<>();
    
    @Builder.Default
    private String status = "pending";
    
    @Builder.Default
    private Integer attempts = 0;
    
    private String lastError;
    
    @Builder.Default
    private Instant availableAt = Instant.now();
    
    private Instant sentAt;
    private Instant createdAt;
    private Instant updatedAt;
}
