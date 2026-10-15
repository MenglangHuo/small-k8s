package com.menglang.ticket.presentation.controller;

import com.menglang.ticket.domain.model.TicketComment;
import com.menglang.ticket.domain.port.in.CommentUseCase;
import com.menglang.ticket.presentation.request.CreateTicketCommentRequest;
import com.menglang.ticket.presentation.request.UpdateTicketCommentRequest;
import com.menglang.ticket.presentation.response.ApiResponse;
import com.menglang.ticket.presentation.response.TicketCommentResponse;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/tickets/{ticketId}/comments")
@RequiredArgsConstructor
public class TicketCommentController {

    private final CommentUseCase commentUseCase;

    @GetMapping
    public Mono<ApiResponse<List<TicketCommentResponse>>> index(
            @PathVariable String ticketId,
            @RequestParam(defaultValue = "true", name = "include_internal") boolean includeInternal,
            ServerWebExchange exchange) {

        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);

        return commentUseCase.listComments(ticketId, companyId, includeInternal)
                .map(TicketCommentResponse::fromDomain)
                .collectList()
                .map(ApiResponse::success);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<TicketCommentResponse>> storeJson(
            @PathVariable String ticketId,
            @Valid @RequestBody CreateTicketCommentRequest request,
            ServerWebExchange exchange) {

        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);
        Long userId = (Long) exchange.getAttributes().getOrDefault("USER_ID", 1L);

        TicketComment comment = TicketComment.builder()
                .ticketId(ticketId)
                .body(request.getBody())
                .isInternal(request.getIsInternal())
                .metadata(request.getMetadata())
                .build();

        return commentUseCase.createComment(ticketId, companyId, comment, Flux.empty(), request.getFiles(), userId)
                .map(created -> ApiResponse.success(TicketCommentResponse.fromDomain(created), "Created successfully."));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ApiResponse<TicketCommentResponse>> storeMultipart(
            @PathVariable String ticketId,
            ServerWebExchange exchange) {

        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);
        Long userId = (Long) exchange.getAttributes().getOrDefault("USER_ID", 1L);

        return exchange.getMultipartData().flatMap(multipartMap -> {
            String body = "";
            List<Part> bodyParts = multipartMap.get("body");
            if (bodyParts != null && !bodyParts.isEmpty() && bodyParts.get(0) instanceof FormFieldPart ffp) {
                body = ffp.value();
            }

            boolean isInternal = false;
            List<Part> internalParts = multipartMap.get("is_internal");
            if (internalParts != null && !internalParts.isEmpty() && internalParts.get(0) instanceof FormFieldPart ffp) {
                isInternal = Boolean.parseBoolean(ffp.value());
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

            TicketComment comment = TicketComment.builder()
                    .ticketId(ticketId)
                    .body(body)
                    .isInternal(isInternal)
                    .build();

            return commentUseCase.createComment(ticketId, companyId, comment, Flux.fromIterable(fileParts), List.of(), userId)
                    .map(created -> ApiResponse.success(TicketCommentResponse.fromDomain(created), "Created successfully."));
        });
    }

    @GetMapping("/{commentId}")
    public Mono<ApiResponse<TicketCommentResponse>> show(
            @PathVariable String ticketId,
            @PathVariable String commentId,
            ServerWebExchange exchange) {

        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);

        return commentUseCase.getComment(ticketId, commentId, companyId)
                .map(comment -> ApiResponse.success(TicketCommentResponse.fromDomain(comment)));
    }

    @PutMapping("/{commentId}")
    public Mono<ApiResponse<TicketCommentResponse>> update(
            @PathVariable String ticketId,
            @PathVariable String commentId,
            @Valid @RequestBody UpdateTicketCommentRequest request,
            ServerWebExchange exchange) {

        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);
        Long userId = (Long) exchange.getAttributes().getOrDefault("USER_ID", 1L);

        return commentUseCase.updateComment(ticketId, commentId, companyId, request.toMap(), userId)
                .map(updated -> ApiResponse.success(TicketCommentResponse.fromDomain(updated), "Updated successfully."));
    }

    @DeleteMapping("/{commentId}")
    public Mono<ApiResponse<Void>> destroy(
            @PathVariable String ticketId,
            @PathVariable String commentId,
            ServerWebExchange exchange) {

        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);
        Long userId = (Long) exchange.getAttributes().getOrDefault("USER_ID", 1L);

        return commentUseCase.deleteComment(ticketId, commentId, companyId, userId)
                .map(deleted -> ApiResponse.success(null, "Deleted successfully."));
    }
}
