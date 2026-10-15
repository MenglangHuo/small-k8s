package com.menglang.ticket.presentation.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientCreateTicketRequest {
    @NotBlank(message = "The ticket title is required.")
    private String title;

    @NotBlank(message = "The ticket description is required.")
    private String description;

    @NotBlank(message = "The ticket type is required (bug, enhancement, question, task).")
    private String type;

    @NotBlank(message = "The ticket priority is required (low, medium, high, critical).")
    private String priority;

    @NotBlank(message = "The reporter name is required.")
    private String reporterName;

    @NotBlank(message = "The reporter phone number is required.")
    private String reporterPhone;

    private String reporterEmail;
    private String externalRef;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @Builder.Default
    private List<String> files = new ArrayList<>();
}
