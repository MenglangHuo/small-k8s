package com.menglang.ticket.infrastructure.persistence.document;

import com.menglang.ticket.domain.model.AssigneeSnapshot;
import com.menglang.ticket.domain.model.ReporterSnapshot;
import com.menglang.ticket.domain.model.TicketPriority;
import com.menglang.ticket.domain.model.TicketStatus;
import com.menglang.ticket.domain.model.TicketType;
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
@Document(collection = "tickets")
@CompoundIndexes({
    @CompoundIndex(name = "tickets_company_active_idx", def = "{'company_id': 1, 'is_deleted': 1, 'created_at': -1}"),
    @CompoundIndex(name = "tickets_company_number_idx", def = "{'company_id': 1, 'number': 1}", unique = true)
})
public class TicketDocument {
    @Id
    private String id;

    @Indexed
    @Field("public_id")
    private String publicId;

    @Indexed
    @Field("company_id")
    private Long companyId;

    @Field("number")
    private Integer number;

    @Indexed
    @Field("ticket_key")
    private String ticketKey;

    @Field("title")
    private String title;

    @Field("description")
    private String description;

    @Field("type")
    @Builder.Default
    private TicketType type = TicketType.BUG;

    @Field("status")
    @Builder.Default
    private TicketStatus status = TicketStatus.OPEN;

    @Field("priority")
    @Builder.Default
    private TicketPriority priority = TicketPriority.MEDIUM;

    @Field("source")
    private String source;

    @Field("external_ref")
    private String externalRef;

    @Field("reporter_name")
    private String reporterName;

    @Field("reporter_email")
    private String reporterEmail;

    @Field("phone")
    private String phone;

    @Field("metadata")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @Field("assignee")
    private AssigneeSnapshot assignee;

    @Field("reporter")
    private ReporterSnapshot reporter;

    @Field("company")
    private String company;

    @Field("due_at")
    private Instant dueAt;

    @Field("first_response_at")
    private Instant firstResponseAt;

    @Field("resolved_at")
    private Instant resolvedAt;

    @Field("closed_at")
    private Instant closedAt;

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
