package com.menglang.ticket.presentation.controller;

import com.menglang.ticket.domain.port.in.TelegramUseCase;
import com.menglang.ticket.presentation.response.ApiResponse;
import com.menglang.ticket.presentation.response.TelegramSubscriberResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TelegramController {

    private final TelegramUseCase telegramUseCase;

    @PostMapping({"/api/v1/tickets/telegram/invite-link", "/api/ticket/telegram/invite-link"})
    public Mono<ApiResponse<Map<String, String>>> generateInviteLink(ServerWebExchange exchange) {
        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);
        Long userId = (Long) exchange.getAttributes().getOrDefault("USER_ID", 1L);

        return telegramUseCase.generateInviteLink(companyId, userId)
                .map(link -> ApiResponse.success(Map.of("invite_url", link), "Invite link generated."));
    }

    @GetMapping({"/api/v1/tickets/telegram/counts", "/api/v1/tickets/telegram/stats", "/api/ticket/telegram/counts"})
    public Mono<ApiResponse<Map<String, Object>>> getCounts(ServerWebExchange exchange) {
        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);

        return telegramUseCase.getTelegramCounts(companyId)
                .map(ApiResponse::success);
    }

    @GetMapping({"/api/v1/tickets/telegram/invites/total-count", "/api/v1/tickets/telegram/invites/count", "/api/ticket/telegram/invites/total-count"})
    public Mono<ApiResponse<Map<String, Object>>> getTotalInviteCount(ServerWebExchange exchange) {
        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);

        return telegramUseCase.getTotalInvitesCount(companyId)
                .map(count -> ApiResponse.success(Map.of("total", count, "count", count)));
    }

    @GetMapping({"/api/v1/tickets/telegram/subscribers/total-count", "/api/v1/tickets/telegram/subscribers/count", "/api/ticket/telegram/subscribers/total-count"})
    public Mono<ApiResponse<Map<String, Object>>> getTotalSubscribersCount(ServerWebExchange exchange) {
        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);

        return telegramUseCase.getTotalSubscribersCount(companyId)
                .map(count -> ApiResponse.success(Map.of("total", count, "count", count)));
    }

    @GetMapping({"/api/v1/tickets/telegram/subscribers", "/api/ticket/telegram/subscribers"})
    public Mono<ApiResponse<List<TelegramSubscriberResponse>>> listSubscribers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10", name = "per_page") int perPage,
            ServerWebExchange exchange) {

        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);

        return telegramUseCase.listSubscribers(companyId, page, perPage)
                .map(TelegramSubscriberResponse::fromDomain)
                .collectList()
                .map(ApiResponse::success);
    }

    @PatchMapping({"/api/v1/tickets/telegram/subscribers/{chatId}/status", "/api/ticket/telegram/subscribers/{chatId}/status"})
    public Mono<ApiResponse<TelegramSubscriberResponse>> updateSubscriberStatus(
            @PathVariable String chatId,
            @RequestBody(required = false) Map<String, Object> body,
            ServerWebExchange exchange) {

        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);
        Boolean isActive = (body != null && body.containsKey("is_active"))
                ? Boolean.parseBoolean(String.valueOf(body.get("is_active")))
                : null;

        return telegramUseCase.updateSubscriberStatus(chatId, companyId, isActive)
                .map(sub -> ApiResponse.success(TelegramSubscriberResponse.fromDomain(sub), "Telegram notification status updated successfully."));
    }

    @PostMapping({"/api/v1/tickets/telegram/subscribers/{chatId}/test", "/api/v1/tickets/telegram/test-notification", "/api/ticket/telegram/subscribers/{chatId}/test"})
    public Mono<ApiResponse<Map<String, Object>>> sendTestNotification(
            @PathVariable(required = false) String chatId,
            @RequestBody(required = false) Map<String, Object> body,
            ServerWebExchange exchange) {

        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);

        String targetChatId = chatId;
        if (targetChatId == null && body != null) {
            targetChatId = (String) (body.get("chat_id") != null ? body.get("chat_id") : body.get("telegram_chat_id"));
        }

        if (targetChatId == null || targetChatId.isBlank()) {
            return Mono.just(ApiResponse.error("Telegram chat ID is required."));
        }

        return telegramUseCase.sendTestNotification(targetChatId, companyId)
                .map(res -> ApiResponse.success(res, (String) res.getOrDefault("message", "Test notification sent.")));
    }

    @DeleteMapping({"/api/v1/tickets/telegram/subscribers/{chatId}", "/api/ticket/telegram/subscribers/{chatId}"})
    public Mono<ApiResponse<Void>> removeSubscriber(
            @PathVariable String chatId,
            ServerWebExchange exchange) {

        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);

        return telegramUseCase.removeSubscriber(chatId, companyId)
                .map(removed -> ApiResponse.success(null, "Subscriber removed."));
    }

    /**
     * Public Telegram Bot Webhook endpoint.
     */
    @PostMapping("/api/v1/telegram/webhook")
    public Mono<Map<String, Object>> webhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secretToken) {

        return telegramUseCase.handleWebhook(payload, secretToken);
    }
}
