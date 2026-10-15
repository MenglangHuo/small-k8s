package com.menglang.ticket.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiClient {
    private String id;
    private Long companyId;
    private String clientId;
    private String clientSecret;
    private String name;
    
    @Builder.Default
    private Boolean isActive = true;
    
    private Instant createdAt;
    private Instant updatedAt;
}
