package com.menglang.ticket.domain.port.out;

import reactor.core.publisher.Mono;

public interface TelegramPort {
    Mono<Boolean> sendMessage(String chatId, String htmlMessage);
}
