package com.menglang.ticket.application.service;

import com.menglang.ticket.domain.model.TicketAttachment;
import com.menglang.ticket.domain.port.in.AttachmentUseCase;
import com.menglang.ticket.domain.port.out.AttachmentRepositoryPort;
import com.menglang.ticket.domain.port.out.StoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentApplicationService implements AttachmentUseCase {

    private final AttachmentRepositoryPort attachmentRepositoryPort;
    private final StoragePort storagePort;

    @Override
    public Flux<TicketAttachment> listAttachments(String ticketId, Long companyId) {
        return attachmentRepositoryPort.findByTicketId(ticketId)
                .map(this::populatePresignedUrl);
    }

    @Override
    public Flux<TicketAttachment> uploadAttachments(String ticketId, Long companyId, String commentId,
                                                   Flux<FilePart> files, List<String> base64Files, Long actorId) {
        Flux<TicketAttachment> multipartFlux = (files != null) ? files.flatMap(filePart -> {
            String originalFilename = filePart.filename();
            String extension = extractExtension(originalFilename);
            String fileUuid = UUID.randomUUID().toString();
            String storedFileName = fileUuid + "." + extension;
            String s3Key = String.format("ticket/attachments/%d/%s", companyId, storedFileName);
            String contentType = filePart.headers().getContentType() != null 
                    ? filePart.headers().getContentType().toString() 
                    : "application/octet-stream";

            return storagePort.upload(s3Key, filePart, contentType)
                    .flatMap(key -> {
                        TicketAttachment attachment = TicketAttachment.builder()
                                .ticketId(ticketId)
                                .commentId(commentId)
                                .fileName(originalFilename)
                                .filePath(key)
                                .mimeType(contentType)
                                .uploadedBy(actorId)
                                .isDeleted(false)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build();
                        return attachmentRepositoryPort.save(attachment);
                    })
                    .map(this::populatePresignedUrl);
        }) : Flux.empty();

        Flux<TicketAttachment> base64Flux = (base64Files != null && !base64Files.isEmpty()) 
                ? Flux.fromIterable(base64Files).flatMap(base64Str -> processBase64Upload(ticketId, companyId, commentId, base64Str, actorId))
                : Flux.empty();

        return Flux.concat(multipartFlux, base64Flux);
    }

    private Mono<TicketAttachment> processBase64Upload(String ticketId, Long companyId, String commentId, String base64Payload, Long actorId) {
        try {
            String contentType = "image/png";
            String rawBase64 = base64Payload;
            String extension = "png";

            if (base64Payload.startsWith("data:") && base64Payload.contains(";base64,")) {
                int commaIdx = base64Payload.indexOf(";base64,");
                String header = base64Payload.substring(5, commaIdx);
                contentType = header;
                rawBase64 = base64Payload.substring(commaIdx + 8);
                if (contentType.contains("/")) {
                    extension = contentType.substring(contentType.indexOf('/') + 1);
                }
            }

            byte[] decodedBytes = Base64.getDecoder().decode(rawBase64.trim());
            String storedFileName = UUID.randomUUID() + "." + extension;
            String s3Key = String.format("ticket/attachments/%d/%s", companyId, storedFileName);

            String finalContentType = contentType;
            return storagePort.uploadBase64(s3Key, decodedBytes, finalContentType)
                    .flatMap(key -> {
                        TicketAttachment attachment = TicketAttachment.builder()
                                .ticketId(ticketId)
                                .commentId(commentId)
                                .fileName(storedFileName)
                                .filePath(key)
                                .fileSize((long) decodedBytes.length)
                                .mimeType(finalContentType)
                                .uploadedBy(actorId)
                                .isDeleted(false)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build();
                        return attachmentRepositoryPort.save(attachment);
                    })
                    .map(this::populatePresignedUrl);
        } catch (Exception e) {
            log.error("Failed to decode base64 file upload: {}", e.getMessage());
            return Mono.empty();
        }
    }

    @Override
    public Mono<Boolean> deleteAttachment(String ticketId, String attachmentId, Long companyId, Long actorId) {
        return attachmentRepositoryPort.softDelete(attachmentId, ticketId);
    }

    private TicketAttachment populatePresignedUrl(TicketAttachment attachment) {
        if (attachment != null && attachment.getFilePath() != null) {
            attachment.setFileUrl(storagePort.generatePresignedUrl(attachment.getFilePath()));
        }
        return attachment;
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "bin";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
