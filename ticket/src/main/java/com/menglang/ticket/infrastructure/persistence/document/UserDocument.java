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
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
@CompoundIndexes({
        @CompoundIndex(name = "users_company_email_idx", def = "{'company_id': 1, 'email': 1}", unique = true),
        @CompoundIndex(name = "users_company_user_id_idx", def = "{'company_id': 1, 'user_id': 1}", unique = true)
})
public class UserDocument {

    @Id
    private String id;

    @Field("user_id")
    private Long userId;

    @Field("company_id")
    private Long companyId;

    private String username;

    @Field("full_name")
    private String fullName;

    private String email;
    private String phone;

    @Field("avatar_url")
    private String avatarUrl;

    @Field("password_hash")
    private String passwordHash;

    @Field("role_ids")
    @Builder.Default
    private List<Long> roleIds = new ArrayList<>();

    @Field("role_names")
    @Builder.Default
    private List<String> roleNames = new ArrayList<>();

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
