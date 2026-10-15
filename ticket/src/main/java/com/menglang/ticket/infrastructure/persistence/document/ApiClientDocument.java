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
@Document(collection = "api_clients")
@CompoundIndexes({
    @CompoundIndex(name = "api_clients_company_active_idx", def = "{'company_id': 1, 'is_active': 1}")
})
public class ApiClientDocument {
    @Id
    private String id;

    @Indexed
    @Field("company_id")
    private Long companyId;

    @Indexed(unique = true)
    @Field("client_id")
    private String clientId;

    @Field("client_secret")
    private String clientSecret;

    @Field("name")
    private String name;

    @Field("is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;
}
