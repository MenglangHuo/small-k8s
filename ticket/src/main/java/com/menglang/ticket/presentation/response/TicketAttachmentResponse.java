package com.menglang.ticket.presentation.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.menglang.ticket.domain.model.TicketAttachment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TicketAttachmentResponse {
    private String id;
    private String ticketId;
    private String commentId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;
    private Long uploadedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Phnom_Penh")
    private Instant createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Phnom_Penh")
    private Instant updatedAt;

    public static TicketAttachmentResponse fromDomain(TicketAttachment att) {
        if (att == null) return null;
        return TicketAttachmentResponse.builder()
                .id(att.getId())
                .ticketId(att.getTicketId())
                .commentId(att.getCommentId())
                .fileName(att.getFileName())
                .fileUrl(att.getFileUrl())
                .fileSize(att.getFileSize())
                .mimeType(att.getMimeType())
                .uploadedBy(att.getUploadedBy())
                .createdAt(att.getCreatedAt())
                .updatedAt(att.getUpdatedAt())
                .build();
    }
}
