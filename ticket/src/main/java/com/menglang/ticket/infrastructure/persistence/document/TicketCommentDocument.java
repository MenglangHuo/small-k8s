package com.menglang.ticket.infrastructure.persistence.document;

import com.menglang.ticket.domain.model.ReporterSnapshot;
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
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ticket_comments")
@CompoundIndexes({
    @CompoundIndex(name = "comments_ticket_active_idx", def = "{'ticket_id': 1, 'is_deleted': 1, 'created_at': 1}")
})
public class TicketCommentDocument {
    @Id
    private String id;

    @Indexed
    @Field("ticket_id")
    private String ticketId;

    @Field("user_id")
    private Long userId;

    @Field("body")
    private String body;

    @Field("is_internal")
    @Builder.Default
    private Boolean isInternal = false;

    @Field("author")
    private ReporterSnapshot author;

    @Field("metadata")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @Field("is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @Field("created_by")
    private Long createdBy;

    @Field("updated_by")
    private Long updatedBy;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;
}
