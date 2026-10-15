package com.menglang.ticket.presentation.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketCommentRequest {
    @NotBlank(message = "Comment body cannot be empty.")
    private String body;

    private Boolean isInternal;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (body != null) map.put("body", body);
        if (isInternal != null) map.put("is_internal", isInternal);
        if (metadata != null && !metadata.isEmpty()) map.put("metadata", metadata);
        return map;
    }
}
