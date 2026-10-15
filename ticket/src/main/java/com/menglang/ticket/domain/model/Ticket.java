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
public class Ticket {
    private String id;
    private String publicId;
    private Long companyId;
    private Integer number;
    private String ticketKey;
    private String title;
    private String description;
    
    @Builder.Default
    private TicketType type = TicketType.BUG;
    
    @Builder.Default
    private TicketStatus status = TicketStatus.OPEN;
    
    @Builder.Default
    private TicketPriority priority = TicketPriority.MEDIUM;
    
    private String source;
    private String externalRef;
    private String reporterName;
    private String reporterEmail;
    private String phone;
    
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    private AssigneeSnapshot assignee;
    private ReporterSnapshot reporter;
    private String company;
    
    private Instant dueAt;
    private Instant firstResponseAt;
    private Instant resolvedAt;
    private Instant closedAt;
    
    @Builder.Default
    private Boolean isDeleted = false;
    
    private Long createdBy;
    private Long updatedBy;
    
    private Instant createdAt;
    private Instant updatedAt;
    
    @Builder.Default
    private List<TicketAttachment> attachments = new ArrayList<>();
    
    @Builder.Default
    private List<TicketComment> comments = new ArrayList<>();
}
