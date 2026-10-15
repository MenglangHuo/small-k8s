package com.menglang.ticket.presentation.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateApiClientRequest {
    @NotBlank(message = "Client name is required.")
    private String name;

    private String clientId;
    private String clientSecret;
    private Long companyId;

    @Builder.Default
    private Boolean isActive = true;
}
