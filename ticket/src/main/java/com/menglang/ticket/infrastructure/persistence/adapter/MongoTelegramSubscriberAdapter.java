package com.menglang.ticket.infrastructure.persistence.adapter;

import com.menglang.ticket.domain.model.TelegramSubscriber;
import com.menglang.ticket.domain.port.out.TelegramSubscriberRepositoryPort;
import com.menglang.ticket.infrastructure.persistence.document.TelegramSubscriberDocument;
import com.menglang.ticket.infrastructure.persistence.repository.SpringDataTelegramSubscriberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class MongoTelegramSubscriberAdapter implements TelegramSubscriberRepositoryPort {

    private final SpringDataTelegramSubscriberRepository subscriberRepository;

    @Override
    public Mono<TelegramSubscriber> save(TelegramSubscriber subscriber) {
        TelegramSubscriberDocument doc = TicketDocMapper.toDocument(subscriber);
        return subscriberRepository.save(doc)
                .map(TicketDocMapper::toDomain);
    }

    @Override
    public Mono<TelegramSubscriber> findByChatIdAndCompanyId(String chatId, Long companyId) {
        return subscriberRepository.findByTelegramChatIdAndCompanyId(chatId, companyId)
                .map(TicketDocMapper::toDomain);
    }

    @Override
    public Flux<TelegramSubscriber> findActiveByCompanyId(Long companyId) {
        return subscriberRepository.findByCompanyIdAndIsActiveTrue(companyId)
                .map(TicketDocMapper::toDomain);
    }

    @Override
    public Flux<TelegramSubscriber> findByCompanyId(Long companyId, int page, int perPage) {
        int p = Math.max(page - 1, 0);
        int size = perPage > 0 ? perPage : 20;
        return subscriberRepository.findByCompanyIdOrderBySubscribedAtDesc(companyId, PageRequest.of(p, size))
                .map(TicketDocMapper::toDomain);
    }

    @Override
    public Mono<Long> countByCompanyId(Long companyId, Boolean activeOnly) {
        if (Boolean.TRUE.equals(activeOnly)) {
            return subscriberRepository.countByCompanyIdAndIsActiveTrue(companyId);
        }
        return subscriberRepository.countByCompanyId(companyId);
    }

    @Override
    public Mono<Boolean> remove(String chatId, Long companyId) {
        return subscriberRepository.findByTelegramChatIdAndCompanyId(chatId, companyId)
                .flatMap(doc -> {
                    doc.setIsActive(false);
                    return subscriberRepository.save(doc).thenReturn(true);
                })
                .defaultIfEmpty(false);
    }
}
