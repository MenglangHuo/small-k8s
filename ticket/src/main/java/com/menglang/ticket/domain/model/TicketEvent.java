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
public class TicketEvent {
    private String id;
    private String ticketId;
    private String eventType;
    private Long actorUserId;
    private String actorType;
    
    @Builder.Default
    private Map<String, Object> payload = new HashMap<>();
    
    private Instant createdAt;
}
