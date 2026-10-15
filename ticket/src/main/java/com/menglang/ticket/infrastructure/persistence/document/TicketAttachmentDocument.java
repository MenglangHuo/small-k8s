package com.menglang.ticket.infrastructure.persistence.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ticket_attachments")
@CompoundIndexes({
    @CompoundIndex(name = "attachments_ticket_idx", def = "{'ticket_id': 1, 'is_deleted': 1}"),
    @CompoundIndex(name = "attachments_comment_idx", def = "{'comment_id': 1, 'is_deleted': 1}")
})
public class TicketAttachmentDocument {
    @Id
    private String id;

    @Indexed
    @Field("ticket_id")
    private String ticketId;

    @Indexed
    @Field("comment_id")
    private String commentId;

    @Field("file_name")
    private String fileName;

    @Field("file_path")
    private String filePath;

    @Field("file_size")
    @Builder.Default
    private Long fileSize = 0L;

    @Field("mime_type")
    private String mimeType;

    @Field("uploaded_by")
    private Long uploadedBy;

    @Field("is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;
}
