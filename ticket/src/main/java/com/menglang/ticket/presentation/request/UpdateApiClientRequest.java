package com.menglang.ticket.presentation.request;

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
public class UpdateApiClientRequest {
    private String name;
    private String clientId;
    private String clientSecret;
    private Boolean regenerateSecret;
    private Boolean isActive;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (name != null) map.put("name", name);
        if (clientId != null) map.put("client_id", clientId);
        if (clientSecret != null) map.put("client_secret", clientSecret);
        if (regenerateSecret != null) map.put("regenerate_secret", regenerateSecret);
        if (isActive != null) map.put("is_active", isActive);
        return map;
    }
}
