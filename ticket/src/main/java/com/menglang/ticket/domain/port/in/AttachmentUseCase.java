package com.menglang.ticket.domain.port.in;

import com.menglang.ticket.domain.model.TicketAttachment;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface AttachmentUseCase {
    Flux<TicketAttachment> listAttachments(String ticketId, Long companyId);
    Flux<TicketAttachment> uploadAttachments(String ticketId, Long companyId, String commentId, Flux<FilePart> files, List<String> base64Files, Long actorId);
    Mono<Boolean> deleteAttachment(String ticketId, String attachmentId, Long companyId, Long actorId);
}
