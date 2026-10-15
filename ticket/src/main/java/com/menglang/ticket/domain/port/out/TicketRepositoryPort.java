package com.menglang.ticket.domain.port.out;

import com.menglang.ticket.domain.model.DashboardReportResult;
import com.menglang.ticket.domain.model.Ticket;
import com.menglang.ticket.domain.model.TicketFacetResult;
import com.menglang.ticket.domain.model.TicketQuery;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface TicketRepositoryPort {
    Mono<Ticket> save(Ticket ticket);
    Mono<Ticket> findById(String id, Long companyId);
    Mono<Ticket> findByTicketKey(String ticketKey, Long companyId);
    Mono<TicketFacetResult> listWithFacets(TicketQuery query);
    Mono<DashboardReportResult> getDashboardMetrics(Long companyId, Map<String, Object> filters);
    Mono<Boolean> softDelete(String id, Long companyId, Long deletedBy);
}
