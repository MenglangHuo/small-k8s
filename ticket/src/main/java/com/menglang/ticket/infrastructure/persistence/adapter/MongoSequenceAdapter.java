package com.menglang.ticket.infrastructure.persistence.adapter;

import com.menglang.ticket.domain.port.out.SequenceRepositoryPort;
import com.menglang.ticket.infrastructure.persistence.document.TicketSequenceDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class MongoSequenceAdapter implements SequenceRepositoryPort {

    private final ReactiveMongoTemplate mongoTemplate;

    @Override
    public Mono<String> nextTicketKey(Long companyId, String prefix) {
        String cleanPrefix = (prefix != null && !prefix.isBlank()) ? prefix.trim() : "TCK";
        Query query = Query.query(Criteria.where("_id").is(companyId));
        Update update = new Update().inc("last_number", 1);
        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true).upsert(true);

        return mongoTemplate.findAndModify(query, update, options, TicketSequenceDocument.class)
                .map(seq -> String.format("%s-%d", cleanPrefix, seq.getLastNumber()));
    }
}
