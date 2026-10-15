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
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TicketListResponse {
    private String id;
    private String publicId;
    private String ticketKey;
    private Integer number;
    private String title;
    private String descriptionPreview;
    private String type;
    private String status;
    private String priority;
    private String source;
    private String externalRef;
    private String reporterName;
    private String reporterEmail;
    private String company;
    private String reporterPhone;
    private Map<String, Object> metadata;
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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Phnom_Penh")
    private Instant createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Phnom_Penh")
    private Instant updatedAt;

    public static TicketListResponse fromDomain(Ticket ticket) {
        if (ticket == null) return null;
        String desc = ticket.getDescription() != null ? ticket.getDescription() : "";
        String preview = desc.length() > 200 ? desc.substring(0, 200) + "..." : desc;

        return TicketListResponse.builder()
                .id(ticket.getId())
                .publicId(ticket.getPublicId())
                .ticketKey(ticket.getTicketKey())
                .number(ticket.getNumber())
                .title(ticket.getTitle())
                .descriptionPreview(preview)
                .type(ticket.getType() != null ? ticket.getType().getValue() : "bug")
                .status(ticket.getStatus() != null ? ticket.getStatus().getValue() : "open")
                .priority(ticket.getPriority() != null ? ticket.getPriority().getValue() : "medium")
                .source(ticket.getSource())
                .externalRef(ticket.getExternalRef())
                .reporterName(ticket.getReporterName())
                .reporterEmail(ticket.getReporterEmail())
                .company(ticket.getCompany() != null ? ticket.getCompany() : "UYFC")
                .reporterPhone(ticket.getPhone())
                .metadata(ticket.getMetadata())
                .assignee(ticket.getAssignee())
                .reporter(ticket.getReporter())
                .dueAt(ticket.getDueAt())
                .firstResponseAt(ticket.getFirstResponseAt())
                .resolvedAt(ticket.getResolvedAt())
                .closedAt(ticket.getClosedAt())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }
}
