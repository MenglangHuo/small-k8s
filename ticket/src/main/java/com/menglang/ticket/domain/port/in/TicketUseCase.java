package com.menglang.ticket.domain.port.in;

import com.menglang.ticket.domain.model.DashboardReportResult;
import com.menglang.ticket.domain.model.Ticket;
import com.menglang.ticket.domain.model.TicketFacetResult;
import com.menglang.ticket.domain.model.TicketQuery;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface TicketUseCase {
    Mono<Ticket> createTicket(Ticket ticket, Flux<FilePart> files, List<String> base64Files);
    Mono<Ticket> getTicketById(String id, Long companyId, boolean includeInternalComments);
    Mono<TicketFacetResult> listTickets(TicketQuery query);
    Mono<Ticket> updateTicket(String id, Long companyId, Map<String, Object> updateFields, Long actorId);
    Mono<Ticket> updateStatus(String id, Long companyId, String newStatus, Long actorId);
    Mono<Boolean> deleteTicket(String id, Long companyId, Long actorId);
    Mono<DashboardReportResult> getDashboardReports(Long companyId, Map<String, Object> filters);
}
