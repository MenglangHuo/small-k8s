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
@Document(collection = "telegram_invites")
@CompoundIndexes({
    @CompoundIndex(name = "invites_valid_idx", def = "{'token': 1, 'is_used': 1, 'expires_at': 1}")
})
public class TelegramInviteDocument {
    @Id
    private String id;

    @Indexed
    @Field("company_id")
    private Long companyId;

    @Indexed(unique = true)
    @Field("token")
    private String token;

    @Field("is_used")
    @Builder.Default
    private Boolean isUsed = false;

    @Field("expires_at")
    private Instant expiresAt;

    @Field("created_by")
    private Long createdBy;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;
}
