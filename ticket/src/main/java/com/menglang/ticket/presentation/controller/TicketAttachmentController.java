package com.menglang.ticket.presentation.controller;

import com.menglang.ticket.domain.port.in.AttachmentUseCase;
import com.menglang.ticket.presentation.response.ApiResponse;
import com.menglang.ticket.presentation.response.TicketAttachmentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping({"/api/v1/tickets/{ticketId}/attachments", "/api/ticket/{ticketId}/attachments"})
@RequiredArgsConstructor
public class TicketAttachmentController {

    private final AttachmentUseCase attachmentUseCase;

    @GetMapping
    public Mono<ApiResponse<List<TicketAttachmentResponse>>> index(
            @PathVariable String ticketId,
            ServerWebExchange exchange) {

        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);

        return attachmentUseCase.listAttachments(ticketId, companyId)
                .map(TicketAttachmentResponse::fromDomain)
                .collectList()
                .map(ApiResponse::success);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ApiResponse<List<TicketAttachmentResponse>>> store(
            @PathVariable String ticketId,
            ServerWebExchange exchange) {

        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);
        Long userId = (Long) exchange.getAttributes().getOrDefault("USER_ID", 1L);

        return exchange.getMultipartData().flatMap(multipartMap -> {
            String commentId = null;
            List<Part> commentParts = multipartMap.get("comment_id");
            if (commentParts != null && !commentParts.isEmpty() && commentParts.get(0) instanceof FormFieldPart ffp) {
                commentId = ffp.value();
            }

            List<FilePart> fileParts = new ArrayList<>();
            for (String key : List.of("files", "files[]", "file", "attachments", "attachments[]", "attachment")) {
                List<Part> parts = multipartMap.get(key);
                if (parts != null) {
                    for (Part part : parts) {
                        if (part instanceof FilePart fp) {
                            fileParts.add(fp);
                        }
                    }
                }
            }

            return attachmentUseCase.uploadAttachments(ticketId, companyId, commentId, Flux.fromIterable(fileParts), List.of(), userId)
                    .map(TicketAttachmentResponse::fromDomain)
                    .collectList()
                    .map(list -> ApiResponse.success(list, "Created successfully."));
        });
    }

    @DeleteMapping("/{attachmentId}")
    public Mono<ApiResponse<Void>> destroy(
            @PathVariable String ticketId,
            @PathVariable String attachmentId,
            ServerWebExchange exchange) {

        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);
        Long userId = (Long) exchange.getAttributes().getOrDefault("USER_ID", 1L);

        return attachmentUseCase.deleteAttachment(ticketId, attachmentId, companyId, userId)
                .map(deleted -> ApiResponse.success(null, "Deleted successfully."));
    }
}
