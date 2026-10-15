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
public class User {
    private String id;
    private Long userId;
    private Long companyId;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String passwordHash;
    
    @Builder.Default
    private List<Long> roleIds = new ArrayList<>();
    
    @Builder.Default
    private List<String> roleNames = new ArrayList<>();
    
    @Builder.Default
    private Boolean isActive = true;
    
    private Instant createdAt;
    private Instant updatedAt;
}
