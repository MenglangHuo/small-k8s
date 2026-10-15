package com.menglang.ticket.infrastructure.persistence.adapter;

import com.menglang.ticket.domain.model.TicketEvent;
import com.menglang.ticket.domain.port.out.EventRepositoryPort;
import com.menglang.ticket.infrastructure.persistence.document.TicketEventDocument;
import com.menglang.ticket.infrastructure.persistence.repository.SpringDataEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class MongoEventRepositoryAdapter implements EventRepositoryPort {

    private final SpringDataEventRepository eventRepository;

    @Override
    public Mono<TicketEvent> record(TicketEvent event) {
        TicketEventDocument doc = TicketDocMapper.toDocument(event);
        return eventRepository.save(doc)
                .map(saved -> {
                    event.setId(saved.getId());
                    event.setCreatedAt(saved.getCreatedAt());
                    return event;
                });
    }
}
