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
public class TicketAttachment {
    private String id;
    private String ticketId;
    private String commentId;
    private String fileName;
    private String filePath;
    private String fileUrl;
    
    @Builder.Default
    private Long fileSize = 0L;
    
    private String mimeType;
    private Long uploadedBy;
    
    @Builder.Default
    private Boolean isDeleted = false;
    
    private Instant createdAt;
    private Instant updatedAt;
}
