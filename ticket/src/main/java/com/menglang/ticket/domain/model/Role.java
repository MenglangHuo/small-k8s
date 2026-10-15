package com.menglang.ticket.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    private String id;
    private Long roleId;
    private Long companyId;
    private String name;
    private String code;
    private String description;
    
    @Builder.Default
    private List<String> permissions = new ArrayList<>();
    
    @Builder.Default
    private Boolean isSystem = false;
    
    private Instant createdAt;
    private Instant updatedAt;
}
