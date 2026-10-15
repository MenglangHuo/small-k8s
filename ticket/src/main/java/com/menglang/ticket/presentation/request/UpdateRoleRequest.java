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
public class UpdateRoleRequest {

    private String name;
    private String description;
    private List<String> permissions;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (name != null) map.put("name", name);
        if (description != null) map.put("description", description);
        if (permissions != null) map.put("permissions", permissions);
        return map;
    }
}
