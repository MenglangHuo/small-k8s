package com.menglang.ticket.presentation.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    private String fullName;
    private String phone;
    private String avatarUrl;
    private String password;
    private List<Long> roleIds;
    private List<String> roleNames;
    private Boolean isActive;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (fullName != null) map.put("full_name", fullName);
        if (phone != null) map.put("phone", phone);
        if (avatarUrl != null) map.put("avatar_url", avatarUrl);
        if (roleIds != null) map.put("role_ids", roleIds);
        if (roleNames != null) map.put("role_names", roleNames);
        if (isActive != null) map.put("is_active", isActive);
        return map;
    }
}
