package com.menglang.ticket.presentation.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.menglang.ticket.domain.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleResponse {
    private String id;
    private Long roleId;
    private Long companyId;
    private String name;
    private String code;
    private String description;
    private List<String> permissions;
    private Boolean isSystem;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Phnom_Penh")
    private Instant createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Phnom_Penh")
    private Instant updatedAt;

    public static RoleResponse fromDomain(Role r) {
        if (r == null) return null;
        return RoleResponse.builder()
                .id(r.getId())
                .roleId(r.getRoleId())
                .companyId(r.getCompanyId())
                .name(r.getName())
                .code(r.getCode())
                .description(r.getDescription())
                .permissions(r.getPermissions())
                .isSystem(r.getIsSystem())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
