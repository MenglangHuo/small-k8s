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
@Document(collection = "roles")
@CompoundIndexes({
        @CompoundIndex(name = "roles_company_code_idx", def = "{'company_id': 1, 'code': 1}", unique = true)
})
public class RoleDocument {

    @Id
    private String id;

    @Field("role_id")
    private Long roleId;

    @Field("company_id")
    private Long companyId;

    private String name;
    private String code;
    private String description;

    @Builder.Default
    private List<String> permissions = new ArrayList<>();

    @Field("is_system")
    @Builder.Default
    private Boolean isSystem = false;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;
}
