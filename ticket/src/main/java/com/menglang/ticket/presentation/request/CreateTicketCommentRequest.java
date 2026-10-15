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
public class CreateTicketCommentRequest {
    @NotBlank(message = "Comment body is required.")
    private String body;

    @Builder.Default
    private Boolean isInternal = false;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @Builder.Default
    private List<String> files = new ArrayList<>();
}
