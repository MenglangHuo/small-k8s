package com.menglang.ticket.application.service;

import com.menglang.ticket.domain.exception.ResourceNotFoundException;
import com.menglang.ticket.domain.model.TicketAttachment;
import com.menglang.ticket.domain.model.TicketComment;
import com.menglang.ticket.domain.model.TicketEvent;
import com.menglang.ticket.domain.port.in.AttachmentUseCase;
import com.menglang.ticket.domain.port.in.CommentUseCase;
import com.menglang.ticket.domain.port.out.CommentRepositoryPort;
import com.menglang.ticket.domain.port.out.EventRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentApplicationService implements CommentUseCase {

    private final CommentRepositoryPort commentRepositoryPort;
    private final AttachmentUseCase attachmentUseCase;
    private final EventRepositoryPort eventRepositoryPort;

    @Override
    public Mono<TicketComment> createComment(String ticketId, Long companyId, TicketComment comment,
                                            Flux<FilePart> files, List<String> base64Files, Long actorId) {
        comment.setTicketId(ticketId);
        comment.setUserId(actorId);
        comment.setIsDeleted(false);
        comment.setCreatedBy(actorId);
        comment.setCreatedAt(Instant.now());
        comment.setUpdatedAt(Instant.now());

        return commentRepositoryPort.save(comment)
                .flatMap(savedComment -> {
                    Mono<List<TicketAttachment>> uploadMono = attachmentUseCase
                            .uploadAttachments(ticketId, companyId, savedComment.getId(), files, base64Files, actorId)
                            .collectList();

                    return uploadMono.flatMap(attachments -> {
                        savedComment.setAttachments(attachments);

                        TicketEvent event = TicketEvent.builder()
                                .ticketId(ticketId)
                                .eventType("comment_added")
                                .actorUserId(actorId)
                                .actorType("user")
                                .payload(Map.of(
                                        "comment_id", savedComment.getId(),
                                        "is_internal", savedComment.getIsInternal(),
                                        "attachments_count", attachments.size()
                                ))
                                .build();

                        return eventRepositoryPort.record(event).thenReturn(savedComment);
                    });
                });
    }

    @Override
    public Flux<TicketComment> listComments(String ticketId, Long companyId, boolean includeInternal) {
        return commentRepositoryPort.findByTicketId(ticketId, includeInternal)
                .flatMap(comment -> attachmentUseCase.listAttachments(ticketId, companyId)
                        .filter(att -> comment.getId().equals(att.getCommentId()))
                        .collectList()
                        .doOnNext(comment::setAttachments)
                        .thenReturn(comment));
    }

    @Override
    public Mono<TicketComment> getComment(String ticketId, String commentId, Long companyId) {
        return commentRepositoryPort.findById(commentId, ticketId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Comment not found with id: " + commentId)))
                .flatMap(comment -> attachmentUseCase.listAttachments(ticketId, companyId)
                        .filter(att -> comment.getId().equals(att.getCommentId()))
                        .collectList()
                        .doOnNext(comment::setAttachments)
                        .thenReturn(comment));
    }

    @Override
    public Mono<TicketComment> updateComment(String ticketId, String commentId, Long companyId, Map<String, Object> fields, Long actorId) {
        return commentRepositoryPort.findById(commentId, ticketId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Comment not found with id: " + commentId)))
                .flatMap(comment -> {
                    if (fields.containsKey("body") && fields.get("body") != null) {
                        comment.setBody(String.valueOf(fields.get("body")));
                    }
                    if (fields.containsKey("is_internal") && fields.get("is_internal") != null) {
                        comment.setIsInternal(Boolean.parseBoolean(String.valueOf(fields.get("is_internal"))));
                    }
                    comment.setUpdatedBy(actorId);
                    comment.setUpdatedAt(Instant.now());

                    return commentRepositoryPort.save(comment);
                });
    }

    @Override
    public Mono<Boolean> deleteComment(String ticketId, String commentId, Long companyId, Long actorId) {
        return commentRepositoryPort.softDelete(commentId, ticketId, actorId);
    }
}
