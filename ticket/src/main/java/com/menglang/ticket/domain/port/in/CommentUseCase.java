package com.menglang.ticket.domain.port.in;

import com.menglang.ticket.domain.model.TicketComment;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface CommentUseCase {
    Mono<TicketComment> createComment(String ticketId, Long companyId, TicketComment comment, Flux<FilePart> files, List<String> base64Files, Long actorId);
    Flux<TicketComment> listComments(String ticketId, Long companyId, boolean includeInternal);
    Mono<TicketComment> getComment(String ticketId, String commentId, Long companyId);
    Mono<TicketComment> updateComment(String ticketId, String commentId, Long companyId, Map<String, Object> fields, Long actorId);
    Mono<Boolean> deleteComment(String ticketId, String commentId, Long companyId, Long actorId);
}
