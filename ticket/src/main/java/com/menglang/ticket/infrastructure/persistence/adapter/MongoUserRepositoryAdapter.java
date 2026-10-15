package com.menglang.ticket.infrastructure.persistence.adapter;

import com.menglang.ticket.domain.model.User;
import com.menglang.ticket.domain.port.out.UserRepositoryPort;
import com.menglang.ticket.infrastructure.persistence.document.UserDocument;
import com.menglang.ticket.infrastructure.persistence.repository.SpringDataUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class MongoUserRepositoryAdapter implements UserRepositoryPort {

    private final SpringDataUserRepository repository;
    private final ReactiveMongoTemplate mongoTemplate;

    @Override
    public Flux<User> findByCompanyIdWithPagination(Long companyId, int skip, int limit, String search) {
        Query query = buildQuery(companyId, search)
                .with(Sort.by(Sort.Direction.DESC, "created_at"))
                .skip(skip)
                .limit(limit);

        return mongoTemplate.find(query, UserDocument.class).map(this::toDomain);
    }

    @Override
    public Mono<Long> countByCompanyId(Long companyId, String search) {
        Query query = buildQuery(companyId, search);
        return mongoTemplate.count(query, UserDocument.class);
    }

    @Override
    public Flux<User> findAgentsByCompanyId(Long companyId) {
        return repository.findActiveByCompanyId(companyId).map(this::toDomain);
    }

    @Override
    public Mono<User> findByIdAndCompanyId(String id, Long companyId) {
        return repository.findByIdAndCompanyId(id, companyId).map(this::toDomain);
    }

    @Override
    public Mono<User> findByUserIdAndCompanyId(Long userId, Long companyId) {
        return repository.findByUserIdAndCompanyId(userId, companyId).map(this::toDomain);
    }

    @Override
    public Mono<User> findByEmail(String email) {
        return repository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public Mono<User> findByUsername(String username) {
        return repository.findByUsername(username).map(this::toDomain);
    }

    @Override
    public Mono<User> save(User user) {
        UserDocument doc = toDocument(user);
        if (doc.getCreatedAt() == null) {
            doc.setCreatedAt(Instant.now());
        }
        doc.setUpdatedAt(Instant.now());
        return repository.save(doc).map(this::toDomain);
    }

    @Override
    public Mono<Long> count() {
        return repository.count();
    }

    private Query buildQuery(Long companyId, String search) {
        Criteria criteria = Criteria.where("company_id").is(companyId);

        if (search != null && !search.isBlank()) {
            Pattern pattern = Pattern.compile(Pattern.quote(search.trim()), Pattern.CASE_INSENSITIVE);
            criteria = criteria.andOperator(
                    new Criteria().orOperator(
                            Criteria.where("full_name").regex(pattern),
                            Criteria.where("username").regex(pattern),
                            Criteria.where("email").regex(pattern)
                    )
            );
        }

        return Query.query(criteria);
    }

    private User toDomain(UserDocument doc) {
        if (doc == null) return null;
        return User.builder()
                .id(doc.getId())
                .userId(doc.getUserId())
                .companyId(doc.getCompanyId())
                .username(doc.getUsername())
                .fullName(doc.getFullName())
                .email(doc.getEmail())
                .phone(doc.getPhone())
                .avatarUrl(doc.getAvatarUrl())
                .passwordHash(doc.getPasswordHash())
                .roleIds(doc.getRoleIds())
                .roleNames(doc.getRoleNames())
                .isActive(doc.getIsActive())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }

    private UserDocument toDocument(User domain) {
        if (domain == null) return null;
        return UserDocument.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .companyId(domain.getCompanyId())
                .username(domain.getUsername())
                .fullName(domain.getFullName())
                .email(domain.getEmail())
                .phone(domain.getPhone())
                .avatarUrl(domain.getAvatarUrl())
                .passwordHash(domain.getPasswordHash())
                .roleIds(domain.getRoleIds())
                .roleNames(domain.getRoleNames())
                .isActive(domain.getIsActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
