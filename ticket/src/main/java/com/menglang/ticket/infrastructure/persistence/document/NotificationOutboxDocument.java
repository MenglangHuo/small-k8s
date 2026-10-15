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
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notification_outbox")
@CompoundIndexes({
    @CompoundIndex(name = "outbox_status_available_idx", def = "{'status': 1, 'available_at': 1, 'created_at': 1}")
})
public class NotificationOutboxDocument {
    @Id
    private String id;

    @Indexed
    @Field("ticket_id")
    private String ticketId;

    @Indexed
    @Field("company_id")
    private Long companyId;

    @Field("channel")
    @Builder.Default
    private String channel = "telegram";

    @Field("event_type")
    private String eventType;

    @Field("payload")
    @Builder.Default
    private Map<String, Object> payload = new HashMap<>();

    @Field("status")
    @Builder.Default
    private String status = "pending";

    @Field("attempts")
    @Builder.Default
    private Integer attempts = 0;

    @Field("last_error")
    private String lastError;

    @Field("available_at")
    @Builder.Default
    private Instant availableAt = Instant.now();

    @Field("sent_at")
    private Instant sentAt;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;
}
