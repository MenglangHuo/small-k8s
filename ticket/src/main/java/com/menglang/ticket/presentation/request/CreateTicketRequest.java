package com.menglang.ticket.presentation.request;

import jakarta.validation.constraints.NotBlank;
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
public class CreateTicketRequest {
    @NotBlank(message = "Title is required.")
    private String title;

    private String description;
    private String type;
    private String priority;
    private String source;
    private Long assigneeId;
    private String externalRef;
    private String reporterName;
    private String reporterEmail;
    private String phone;
    private String reporterPhone;
    private Instant dueAt;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
