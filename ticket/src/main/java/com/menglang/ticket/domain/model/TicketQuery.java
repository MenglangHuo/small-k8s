package com.menglang.ticket.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketQuery {
    private Long companyId;
    private String status;
    private String priority;
    private String type;
    private String source;
    private Long assigneeId;
    private String search;
    
    @Builder.Default
    private String sortBy = "id";
    
    @Builder.Default
    private String sortDir = "desc";
    
    @Builder.Default
    private int page = 1;
    
    @Builder.Default
    private int perPage = 20;

    public int getClampedPerPage() {
        return Math.min(Math.max(perPage, 1), 100);
    }

    public int getClampedPage() {
        return Math.max(page, 1);
    }
}
