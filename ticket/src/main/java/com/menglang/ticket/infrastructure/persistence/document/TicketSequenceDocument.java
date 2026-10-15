package com.menglang.ticket.infrastructure.persistence.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ticket_sequences")
public class TicketSequenceDocument {
    @Id
    private Long id; // company_id is the primary ID

    @Field("last_number")
    @Builder.Default
    private Long lastNumber = 0L;
}
