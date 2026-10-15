package com.menglang.ticket.infrastructure.persistence.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
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
@Document(collection = "ticket_events")
public class TicketEventDocument {
    @Id
    private String id;

    @Indexed
    @Field("ticket_id")
    private String ticketId;

    @Field("event_type")
    private String eventType;

    @Field("actor_user_id")
    private Long actorUserId;

    @Field("actor_type")
    private String actorType;

    @Field("payload")
    @Builder.Default
    private Map<String, Object> payload = new HashMap<>();

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;
}
