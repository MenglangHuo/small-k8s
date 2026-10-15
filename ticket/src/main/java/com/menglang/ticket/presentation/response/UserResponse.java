package com.menglang.ticket.presentation.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.menglang.ticket.domain.model.User;
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
public class UserResponse {
    private String id;
    private Long userId;
    private Long companyId;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private List<Long> roleIds;
    private List<String> roleNames;
    private Boolean isActive;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Phnom_Penh")
    private Instant createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Phnom_Penh")
    private Instant updatedAt;

    public static UserResponse fromDomain(User u) {
        if (u == null) return null;
        return UserResponse.builder()
                .id(u.getId())
                .userId(u.getUserId())
                .companyId(u.getCompanyId())
                .username(u.getUsername())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .avatarUrl(u.getAvatarUrl())
                .roleIds(u.getRoleIds())
                .roleNames(u.getRoleNames())
                .isActive(u.getIsActive())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }
}
