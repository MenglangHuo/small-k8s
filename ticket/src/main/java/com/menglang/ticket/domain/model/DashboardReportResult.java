package com.menglang.ticket.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardReportResult {
    @Builder.Default
    private Map<String, Object> summary = new HashMap<>();

    @Builder.Default
    private Map<String, Object> byStatus = new HashMap<>();

    @Builder.Default
    private Map<String, Object> byType = new HashMap<>();

    @Builder.Default
    private Map<String, Object> byPriority = new HashMap<>();

    @Builder.Default
    private List<Map<String, Object>> bySource = new ArrayList<>();

    @Builder.Default
    private List<Map<String, Object>> topClients = new ArrayList<>();

    @Builder.Default
    private List<Map<String, Object>> trends = new ArrayList<>();

    @Builder.Default
    private boolean success = true;
}
