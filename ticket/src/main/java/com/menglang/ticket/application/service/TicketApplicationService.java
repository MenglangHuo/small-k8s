package com.menglang.ticket.application.service;

import com.menglang.ticket.domain.exception.ResourceNotFoundException;
import com.menglang.ticket.domain.model.*;
import com.menglang.ticket.domain.port.in.AttachmentUseCase;
import com.menglang.ticket.domain.port.in.CommentUseCase;
import com.menglang.ticket.domain.port.in.TicketUseCase;
import com.menglang.ticket.domain.port.out.CompanyRepositoryPort;
import com.menglang.ticket.domain.port.out.EventRepositoryPort;
import com.menglang.ticket.domain.port.out.OutboxRepositoryPort;
import com.menglang.ticket.domain.port.out.SequenceRepositoryPort;
import com.menglang.ticket.domain.port.out.TicketRepositoryPort;
import com.menglang.ticket.domain.port.out.UserRepositoryPort;
import com.menglang.ticket.domain.statemachine.TicketStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketApplicationService implements TicketUseCase {

    private final TicketRepositoryPort ticketRepositoryPort;
    private final SequenceRepositoryPort sequenceRepositoryPort;
    private final EventRepositoryPort eventRepositoryPort;
    private final OutboxRepositoryPort outboxRepositoryPort;
    private final AttachmentUseCase attachmentUseCase;
    private final CommentUseCase commentUseCase;
    private final UserRepositoryPort userRepositoryPort;
    private final CompanyRepositoryPort companyRepositoryPort;

    @Value("${ticket.default-prefix:TCK}")
    private String defaultPrefix;

    @Override
    public Mono<Ticket> createTicket(Ticket ticket, Flux<FilePart> files, List<String> base64Files) {
        Long companyId = ticket.getCompanyId() != null ? ticket.getCompanyId() : 1L;
        ticket.setCompanyId(companyId);
        ticket.setPublicId(UUID.randomUUID().toString());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setIsDeleted(false);
        ticket.setCreatedAt(Instant.now());
        ticket.setUpdatedAt(Instant.now());

        Mono<Void> resolveSnapshots = Mono.defer(() -> {
            Mono<Void> compMono = (ticket.getCompany() == null || ticket.getCompany().isBlank())
                    ? companyRepositoryPort.findByCompanyId(companyId)
                            .doOnNext(c -> ticket.setCompany(c.getName()))
                            .then()
                    : Mono.empty();

            Mono<Void> repMono = (ticket.getReporter() == null && ticket.getCreatedBy() != null && ticket.getCreatedBy() > 0)
                    ? userRepositoryPort.findByUserIdAndCompanyId(ticket.getCreatedBy(), companyId)
                            .doOnNext(u -> ticket.setReporter(new ReporterSnapshot(u.getUserId(), u.getFullName())))
                            .then()
                    : Mono.empty();

            return Mono.when(compMono, repMono);
        });

        return resolveSnapshots.then(sequenceRepositoryPort.nextTicketKey(companyId, defaultPrefix))
                .flatMap(key -> {
                    ticket.setTicketKey(key);
                    try {
                        String[] parts = key.split("-");
                        ticket.setNumber(Integer.parseInt(parts[parts.length - 1]));
                    } catch (Exception e) {
                        ticket.setNumber(1);
                    }
                    return ticketRepositoryPort.save(ticket);
                })
                .flatMap(savedTicket -> {
                    // Upload attachments if present
                    Mono<List<TicketAttachment>> uploadMono = attachmentUseCase
                            .uploadAttachments(savedTicket.getId(), companyId, null, files, base64Files, savedTicket.getCreatedBy())
                            .collectList();

                    return uploadMono.flatMap(attachments -> {
                        savedTicket.setAttachments(attachments);

                        // Audit event
                        TicketEvent event = TicketEvent.builder()
                                .ticketId(savedTicket.getId())
                                .eventType("created")
                                .actorUserId(savedTicket.getCreatedBy())
                                .actorType("user")
                                .payload(Map.of(
                                        "initial_status", TicketStatus.OPEN.getValue(),
                                        "initial_priority", savedTicket.getPriority().getValue(),
                                        "source", savedTicket.getSource() != null ? savedTicket.getSource() : "",
                                        "attachments_count", attachments.size()
                                ))
                                .build();

                        Mono<TicketEvent> eventMono = eventRepositoryPort.record(event);

                        // Enqueue Telegram notification if filed via API / client
                        Mono<Void> outboxMono = Mono.empty();
                        if ("api".equalsIgnoreCase(savedTicket.getSource()) || "client".equalsIgnoreCase(savedTicket.getSource())) {
                            NotificationOutbox outbox = NotificationOutbox.builder()
                                    .ticketId(savedTicket.getId())
                                    .companyId(companyId)
                                    .channel("telegram")
                                    .eventType("ticket.created")
                                    .status("pending")
                                    .payload(Map.ofEntries(
                                            Map.entry("ticket_key", savedTicket.getTicketKey() != null ? savedTicket.getTicketKey() : ""),
                                            Map.entry("title", savedTicket.getTitle() != null ? savedTicket.getTitle() : ""),
                                            Map.entry("description", savedTicket.getDescription() != null ? savedTicket.getDescription() : ""),
                                            Map.entry("type", savedTicket.getType() != null ? savedTicket.getType().getValue() : ""),
                                            Map.entry("priority", savedTicket.getPriority() != null ? savedTicket.getPriority().getValue() : ""),
                                            Map.entry("source", savedTicket.getSource() != null ? savedTicket.getSource() : ""),
                                            Map.entry("reporter_name", savedTicket.getReporterName() != null ? savedTicket.getReporterName() : ""),
                                            Map.entry("reporter_phone", savedTicket.getPhone() != null ? savedTicket.getPhone() : ""),
                                            Map.entry("reporter_email", savedTicket.getReporterEmail() != null ? savedTicket.getReporterEmail() : ""),
                                            Map.entry("created_at", savedTicket.getCreatedAt() != null ? savedTicket.getCreatedAt().toString() : ""),
                                            Map.entry("attachments_count", attachments.size())
                                    ))
                                    .build();
                            outboxMono = outboxRepositoryPort.enqueue(outbox).then();
                        }

                        return Mono.when(eventMono, outboxMono).thenReturn(savedTicket);
                    });
                });
    }

    @Override
    public Mono<Ticket> getTicketById(String id, Long companyId, boolean includeInternalComments) {
        return ticketRepositoryPort.findById(id, companyId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Ticket not found with id: " + id)))
                .flatMap(ticket -> attachmentUseCase.listAttachments(ticket.getId(), companyId)
                        .collectList()
                        .doOnNext(ticket::setAttachments)
                        .thenReturn(ticket))
                .flatMap(ticket -> commentUseCase.listComments(ticket.getId(), companyId, includeInternalComments)
                        .collectList()
                        .doOnNext(ticket::setComments)
                        .thenReturn(ticket));
    }

    @Override
    public Mono<TicketFacetResult> listTickets(TicketQuery query) {
        return ticketRepositoryPort.listWithFacets(query);
    }

    @Override
    public Mono<Ticket> updateTicket(String id, Long companyId, Map<String, Object> updateFields, Long actorId) {
        return ticketRepositoryPort.findById(id, companyId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Ticket not found with id: " + id)))
                .flatMap(ticket -> {
                    String oldStatusStr = ticket.getStatus() != null ? ticket.getStatus().getValue() : "open";
                    TicketStatus oldStatus = ticket.getStatus();

                    if (updateFields.containsKey("title") && updateFields.get("title") != null) {
                        ticket.setTitle(String.valueOf(updateFields.get("title")));
                    }
                    if (updateFields.containsKey("description")) {
                        ticket.setDescription((String) updateFields.get("description"));
                    }
                    if (updateFields.containsKey("type") && updateFields.get("type") != null) {
                        ticket.setType(TicketType.fromValue(String.valueOf(updateFields.get("type"))));
                    }
                    if (updateFields.containsKey("priority") && updateFields.get("priority") != null) {
                        ticket.setPriority(TicketPriority.fromValue(String.valueOf(updateFields.get("priority"))));
                    }
                    if (updateFields.containsKey("external_ref")) {
                        ticket.setExternalRef((String) updateFields.get("external_ref"));
                    }
                    if (updateFields.containsKey("phone")) {
                        ticket.setPhone((String) updateFields.get("phone"));
                    }
                    if (updateFields.containsKey("reporter_phone")) {
                        ticket.setPhone((String) updateFields.get("reporter_phone"));
                    }

                    // Check status transition
                    boolean statusChanged = false;
                    if (updateFields.containsKey("status") && updateFields.get("status") != null) {
                        TicketStatus targetStatus = TicketStatus.fromValue(String.valueOf(updateFields.get("status")));
                        if (targetStatus != oldStatus) {
                            TicketStateMachine.assertTransition(oldStatus, targetStatus);
                            ticket.setStatus(targetStatus);
                            statusChanged = true;

                            // Apply SLA timestamps
                            if (targetStatus == TicketStatus.IN_PROGRESS && ticket.getFirstResponseAt() == null) {
                                ticket.setFirstResponseAt(Instant.now());
                            } else if (targetStatus == TicketStatus.RESOLVED) {
                                ticket.setResolvedAt(Instant.now());
                            } else if (targetStatus == TicketStatus.CLOSED) {
                                ticket.setClosedAt(Instant.now());
                            } else if (targetStatus == TicketStatus.OPEN && oldStatus == TicketStatus.RESOLVED) {
                                ticket.setResolvedAt(null);
                            }
                        }
                    }

                    ticket.setUpdatedBy(actorId);
                    ticket.setUpdatedAt(Instant.now());

                    Mono<Void> assigneeMono = Mono.empty();
                    if (updateFields.containsKey("assignee_id")) {
                        Object aIdObj = updateFields.get("assignee_id");
                        if (aIdObj == null || "null".equals(String.valueOf(aIdObj))) {
                            ticket.setAssignee(null);
                        } else {
                            try {
                                Long aId = Long.parseLong(String.valueOf(aIdObj));
                                assigneeMono = userRepositoryPort.findByUserIdAndCompanyId(aId, companyId)
                                        .doOnNext(u -> ticket.setAssignee(new AssigneeSnapshot(u.getUserId(), u.getFullName())))
                                        .then();
                            } catch (Exception ignored) {}
                        }
                    }

                    boolean finalStatusChanged = statusChanged;
                    return assigneeMono.then(ticketRepositoryPort.save(ticket))
                            .flatMap(saved -> {
                                String eventType = finalStatusChanged ? "status_changed" : "updated";
                                Map<String, Object> eventPayload = new HashMap<>();
                                if (finalStatusChanged) {
                                    eventPayload.put("from", oldStatusStr);
                                    eventPayload.put("to", saved.getStatus().getValue());
                                } else {
                                    eventPayload.put("fields", updateFields.keySet());
                                }

                                TicketEvent event = TicketEvent.builder()
                                        .ticketId(saved.getId())
                                        .eventType(eventType)
                                        .actorUserId(actorId)
                                        .actorType("user")
                                        .payload(eventPayload)
                                        .build();

                                return eventRepositoryPort.record(event).thenReturn(saved);
                            });
                });
    }

    @Override
    public Mono<Ticket> updateStatus(String id, Long companyId, String newStatus, Long actorId) {
        return updateTicket(id, companyId, Map.of("status", newStatus), actorId);
    }

    @Override
    public Mono<Boolean> deleteTicket(String id, Long companyId, Long actorId) {
        return ticketRepositoryPort.softDelete(id, companyId, actorId)
                .flatMap(success -> {
                    if (success) {
                        TicketEvent event = TicketEvent.builder()
                                .ticketId(id)
                                .eventType("deleted")
                                .actorUserId(actorId)
                                .actorType("user")
                                .payload(Map.of("deleted_by", actorId))
                                .build();
                        return eventRepositoryPort.record(event).thenReturn(true);
                    }
                    return Mono.just(false);
                });
    }

    @Override
    public Mono<DashboardReportResult> getDashboardReports(Long companyId, Map<String, Object> filters) {
        return ticketRepositoryPort.getDashboardMetrics(companyId, filters);
    }
}
