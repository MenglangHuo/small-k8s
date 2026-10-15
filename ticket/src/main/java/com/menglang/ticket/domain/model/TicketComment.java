package com.menglang.ticket.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketComment {
    private String id;
    private String ticketId;
    private Long userId;
    private String body;
    
    @Builder.Default
    private Boolean isInternal = false;
    
    private ReporterSnapshot author;
    
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    @Builder.Default
    private Boolean isDeleted = false;
    
    private Long createdBy;
    private Long updatedBy;
    
    private Instant createdAt;
    private Instant updatedAt;
    
    @Builder.Default
    private List<TicketAttachment> attachments = new ArrayList<>();
}
