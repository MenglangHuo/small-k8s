package com.menglang.ticket.presentation.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.menglang.ticket.domain.model.ReporterSnapshot;
import com.menglang.ticket.domain.model.TicketComment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TicketCommentResponse {
    private String id;
    private String ticketId;
    private String body;
    private Boolean isInternal;
    private ReporterSnapshot author;
    
    @Builder.Default
    private List<TicketAttachmentResponse> attachments = Collections.emptyList();
    
    private Map<String, Object> metadata;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Phnom_Penh")
    private Instant createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Phnom_Penh")
    private Instant updatedAt;

    public static TicketCommentResponse fromDomain(TicketComment comment) {
        if (comment == null) return null;
        List<TicketAttachmentResponse> attResponses = comment.getAttachments() != null
                ? comment.getAttachments().stream().map(TicketAttachmentResponse::fromDomain).toList()
                : Collections.emptyList();

        return TicketCommentResponse.builder()
                .id(comment.getId())
                .ticketId(comment.getTicketId())
                .body(comment.getBody())
                .isInternal(comment.getIsInternal())
                .author(comment.getAuthor())
                .attachments(attResponses)
                .metadata(comment.getMetadata())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
