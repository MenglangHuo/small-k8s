package com.menglang.ticket.presentation.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.menglang.ticket.domain.model.AssigneeSnapshot;
import com.menglang.ticket.domain.model.ReporterSnapshot;
import com.menglang.ticket.domain.model.Ticket;
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
public class TicketResponse {
    private String id;
    private String publicId;
    private String ticketKey;
    private Integer number;
    private String title;
    private String description;
    private String type;
    private String status;
    private String priority;
    private String source;
    private String externalRef;
    private String reporterName;
    private String reporterEmail;
    private String reporterPhone;
    private AssigneeSnapshot assignee;
    private ReporterSnapshot reporter;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Phnom_Penh")
    private Instant dueAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Phnom_Penh")
    private Instant firstResponseAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Phnom_Penh")
    private Instant resolvedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Phnom_Penh")
    private Instant closedAt;

    private Map<String, Object> metadata;

    @Builder.Default
    private List<TicketAttachmentResponse> attachments = Collections.emptyList();

    @Builder.Default
    private List<TicketCommentResponse> comments = Collections.emptyList();

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Phnom_Penh")
    private Instant createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Phnom_Penh")
    private Instant updatedAt;

    public static TicketResponse fromDomain(Ticket ticket) {
        if (ticket == null) return null;
        List<TicketAttachmentResponse> attResponses = ticket.getAttachments() != null
                ? ticket.getAttachments().stream().map(TicketAttachmentResponse::fromDomain).toList()
                : Collections.emptyList();

        List<TicketCommentResponse> commentResponses = ticket.getComments() != null
                ? ticket.getComments().stream().map(TicketCommentResponse::fromDomain).toList()
                : Collections.emptyList();

        return TicketResponse.builder()
                .id(ticket.getId())
                .publicId(ticket.getPublicId())
                .ticketKey(ticket.getTicketKey())
                .number(ticket.getNumber())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .type(ticket.getType() != null ? ticket.getType().getValue() : "bug")
                .status(ticket.getStatus() != null ? ticket.getStatus().getValue() : "open")
                .priority(ticket.getPriority() != null ? ticket.getPriority().getValue() : "medium")
                .source(ticket.getSource())
                .externalRef(ticket.getExternalRef())
                .reporterName(ticket.getReporterName())
                .reporterEmail(ticket.getReporterEmail())
                .reporterPhone(ticket.getPhone())
                .assignee(ticket.getAssignee())
                .reporter(ticket.getReporter())
                .dueAt(ticket.getDueAt())
                .firstResponseAt(ticket.getFirstResponseAt())
                .resolvedAt(ticket.getResolvedAt())
                .closedAt(ticket.getClosedAt())
                .metadata(ticket.getMetadata())
                .attachments(attResponses)
                .comments(commentResponses)
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }
}
