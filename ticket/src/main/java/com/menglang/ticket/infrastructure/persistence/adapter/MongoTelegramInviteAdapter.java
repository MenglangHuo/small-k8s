package com.menglang.ticket.infrastructure.persistence.adapter;

import com.menglang.ticket.domain.model.TelegramInvite;
import com.menglang.ticket.domain.port.out.TelegramInviteRepositoryPort;
import com.menglang.ticket.infrastructure.persistence.document.TelegramInviteDocument;
import com.menglang.ticket.infrastructure.persistence.repository.SpringDataTelegramInviteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class MongoTelegramInviteAdapter implements TelegramInviteRepositoryPort {

    private final SpringDataTelegramInviteRepository inviteRepository;

    @Override
    public Mono<TelegramInvite> save(TelegramInvite invite) {
        TelegramInviteDocument doc = TicketDocMapper.toDocument(invite);
        return inviteRepository.save(doc)
                .map(TicketDocMapper::toDomain);
    }

    @Override
    public Mono<TelegramInvite> findValidByToken(String token) {
        return inviteRepository.findByTokenAndIsUsedFalseAndExpiresAtGreaterThan(token, Instant.now())
                .map(TicketDocMapper::toDomain);
    }

    @Override
    public Mono<Long> countByCompanyId(Long companyId) {
        return inviteRepository.countByCompanyId(companyId);
    }
}
