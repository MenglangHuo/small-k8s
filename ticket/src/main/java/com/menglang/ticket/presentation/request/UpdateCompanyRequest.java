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
public class UpdateCompanyRequest {

    private String name;
    private String domain;
    private Boolean isActive;
    private Map<String, Object> metadata;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (name != null) map.put("name", name);
        if (domain != null) map.put("domain", domain);
        if (isActive != null) map.put("is_active", isActive);
        if (metadata != null) map.put("metadata", metadata);
        return map;
    }
}
