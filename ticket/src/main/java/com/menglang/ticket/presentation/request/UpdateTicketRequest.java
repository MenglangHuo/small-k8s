package com.menglang.ticket.presentation.request;

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
public class UpdateTicketRequest {
    private String title;
    private String description;
    private String type;
    private String priority;
    private String status;
    private Long assigneeId;
    private String externalRef;
    private String phone;
    private String reporterPhone;
    private Instant dueAt;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    public Map<String, Object> toUpdateMap() {
        Map<String, Object> map = new HashMap<>();
        if (title != null) map.put("title", title);
        if (description != null) map.put("description", description);
        if (type != null) map.put("type", type);
        if (priority != null) map.put("priority", priority);
        if (status != null) map.put("status", status);
        if (assigneeId != null) map.put("assignee_id", assigneeId);
        if (externalRef != null) map.put("external_ref", externalRef);
        if (phone != null) map.put("phone", phone);
        if (reporterPhone != null) map.put("reporter_phone", reporterPhone);
        if (dueAt != null) map.put("due_at", dueAt);
        if (metadata != null && !metadata.isEmpty()) map.put("metadata", metadata);
        return map;
    }
}
