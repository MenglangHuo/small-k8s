package com.menglang.ticket.presentation.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoleRequest {

    @NotBlank(message = "Role name is required")
    private String name;

    @NotBlank(message = "Role code is required")
    private String code;

    private String description;
    private Long companyId;

    @Builder.Default
    private List<String> permissions = new ArrayList<>();
}
