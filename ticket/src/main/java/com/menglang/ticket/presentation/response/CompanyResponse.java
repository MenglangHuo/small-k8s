package com.menglang.ticket.presentation.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.menglang.ticket.domain.model.Company;
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
public class CompanyResponse {
    private String id;
    private Long companyId;
    private String name;
    private String code;
    private String domain;
    private Boolean isActive;
    private Map<String, Object> metadata;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Phnom_Penh")
    private Instant createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Phnom_Penh")
    private Instant updatedAt;

    public static CompanyResponse fromDomain(Company c) {
        if (c == null) return null;
        return CompanyResponse.builder()
                .id(c.getId())
                .companyId(c.getCompanyId())
                .name(c.getName())
                .code(c.getCode())
                .domain(c.getDomain())
                .isActive(c.getIsActive())
                .metadata(c.getMetadata())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
