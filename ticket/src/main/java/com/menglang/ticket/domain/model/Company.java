package com.menglang.ticket.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Company {
    private String id;
    private Long companyId;
    private String name;
    private String code;
    private String domain;
    
    @Builder.Default
    private Boolean isActive = true;
    
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    private Instant createdAt;
    private Instant updatedAt;
}
