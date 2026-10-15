package com.menglang.ticket.presentation.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.menglang.ticket.domain.model.ApiClient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiClientResponse {
    private String id;
    private Long companyId;
    private String clientId;
    private String name;
    private Boolean isActive;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Phnom_Penh")
    private Instant createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Phnom_Penh")
    private Instant updatedAt;

    public static ApiClientResponse fromDomain(ApiClient client) {
        if (client == null) return null;
        return ApiClientResponse.builder()
                .id(client.getId())
                .companyId(client.getCompanyId())
                .clientId(client.getClientId())
                .name(client.getName())
                .isActive(client.getIsActive())
                .createdAt(client.getCreatedAt())
                .updatedAt(client.getUpdatedAt())
                .build();
    }
}
