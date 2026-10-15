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
@Document(collection = "telegram_subscribers")
@CompoundIndexes({
    @CompoundIndex(name = "subscriber_company_chat_idx", def = "{'company_id': 1, 'telegram_chat_id': 1}", unique = true)
})
public class TelegramSubscriberDocument {
    @Id
    private String id;

    @Indexed
    @Field("company_id")
    private Long companyId;

    @Field("telegram_chat_id")
    private String telegramChatId;

    @Field("telegram_username")
    private String telegramUsername;

    @Field("invite_token")
    private String inviteToken;

    @Field("is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Field("subscribed_at")
    @Builder.Default
    private Instant subscribedAt = Instant.now();

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;
}
