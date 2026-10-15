package com.menglang.ticket.application.service;

import com.menglang.ticket.domain.exception.ResourceNotFoundException;
import com.menglang.ticket.domain.model.TelegramInvite;
import com.menglang.ticket.domain.model.TelegramSubscriber;
import com.menglang.ticket.domain.port.in.TelegramUseCase;
import com.menglang.ticket.domain.port.out.TelegramInviteRepositoryPort;
import com.menglang.ticket.domain.port.out.TelegramPort;
import com.menglang.ticket.domain.port.out.TelegramSubscriberRepositoryPort;
import com.menglang.ticket.infrastructure.telegram.TelegramConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramApplicationService implements TelegramUseCase {

    private final TelegramSubscriberRepositoryPort subscriberRepositoryPort;
    private final TelegramInviteRepositoryPort inviteRepositoryPort;
    private final TelegramPort telegramPort;
    private final TelegramConfig telegramConfig;
    private final SecureRandom secureRandom = new SecureRandom();
    private static final Map<String, Object> OK_RESPONSE = Map.of("ok", true);

    @Override
    public Mono<String> generateInviteLink(Long companyId, Long createdBy) {
        String rawToken = generateRandomToken(32);
        String hashedToken = sha256(rawToken);

        TelegramInvite invite = TelegramInvite.builder()
                .companyId(companyId != null ? companyId : 1L)
                .token(hashedToken)
                .isUsed(false)
                .expiresAt(Instant.now().plus(telegramConfig.getInviteExpiryDays(), ChronoUnit.DAYS))
                .createdBy(createdBy)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return inviteRepositoryPort.save(invite)
                .map(saved -> String.format("https://t.me/%s?start=%s", telegramConfig.getBotUsername(), rawToken));
    }

    @Override
    public Flux<TelegramSubscriber> listSubscribers(Long companyId, int page, int perPage) {
        return subscriberRepositoryPort.findByCompanyId(companyId, page, perPage);
    }

    @Override
    public Mono<Map<String, Object>> getTelegramCounts(Long companyId) {
        Mono<Long> totalSubscribersMono = subscriberRepositoryPort.countByCompanyId(companyId, false);
        Mono<Long> activeSubscribersMono = subscriberRepositoryPort.countByCompanyId(companyId, true);
        Mono<Long> totalInvitesMono = inviteRepositoryPort.countByCompanyId(companyId);

        return Mono.zip(totalSubscribersMono, activeSubscribersMono, totalInvitesMono)
                .map(tuple -> {
                    long totalSubs = tuple.getT1();
                    long activeSubs = tuple.getT2();
                    long totalInvites = tuple.getT3();

                    return Map.of(
                            "totalSubscribers", totalSubs,
                            "activeSubscribers", activeSubs,
                            "totalTelegramInvites", totalInvites,
                            "total_subscribers", totalSubs,
                            "active_subscribers", activeSubs,
                            "total_telegram_invites", totalInvites,
                            "total_invites", totalInvites
                    );
                });
    }

    @Override
    public Mono<Long> getTotalInvitesCount(Long companyId) {
        return inviteRepositoryPort.countByCompanyId(companyId);
    }

    @Override
    public Mono<Long> getTotalSubscribersCount(Long companyId) {
        return subscriberRepositoryPort.countByCompanyId(companyId, false);
    }

    @Override
    public Mono<TelegramSubscriber> updateSubscriberStatus(String chatId, Long companyId, Boolean isActive) {
        return subscriberRepositoryPort.findByChatIdAndCompanyId(chatId, companyId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Subscriber not found for chatId: " + chatId)))
                .flatMap(sub -> {
                    boolean targetState = isActive != null ? isActive : !Boolean.TRUE.equals(sub.getIsActive());
                    sub.setIsActive(targetState);
                    sub.setUpdatedAt(Instant.now());

                    String msg = targetState
                            ? "🔔 <b>Notifications Enabled:</b> You will now receive ticket updates."
                            : "🔕 <b>Notifications Muted:</b> Ticket alerts have been muted for this chat.";

                    return subscriberRepositoryPort.save(sub)
                            .flatMap(saved -> telegramPort.sendMessage(chatId, msg).thenReturn(saved));
                });
    }

    @Override
    public Mono<Map<String, Object>> sendTestNotification(String chatId, Long companyId) {
        String msg = "🧪 <b>TEST NOTIFICATION</b>\n━━━━━━━━━━━━━━━━━━━━\nYour connection to the Ticket Management Bot is active and verified!";
        return telegramPort.sendMessage(chatId, msg)
                .map(success -> Map.of(
                        "success", success,
                        "chat_id", chatId,
                        "message", success ? "Test notification sent successfully." : "Failed to deliver test notification."
                ));
    }

    @Override
    public Mono<Boolean> removeSubscriber(String chatId, Long companyId) {
        return subscriberRepositoryPort.remove(chatId, companyId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> handleWebhook(Map<String, Object> updatePayload, String secretTokenHeader) {
        if (telegramConfig.getWebhookSecret() != null && !telegramConfig.getWebhookSecret().isBlank()) {
            if (!telegramConfig.getWebhookSecret().equals(secretTokenHeader)) {
                log.warn("Telegram webhook secret mismatch.");
                return Mono.just(Map.of("ok", false, "error", "Unauthorized"));
            }
        }

        Map<String, Object> message = (Map<String, Object>) updatePayload.get("message");
        if (message == null || !message.containsKey("text")) {
            return Mono.just(OK_RESPONSE);
        }

        String text = String.valueOf(message.get("text")).trim();
        Map<String, Object> chat = (Map<String, Object>) message.get("chat");
        String chatId = chat != null ? String.valueOf(chat.get("id")) : "";

        Map<String, Object> from = (Map<String, Object>) message.get("from");
        String username = from != null ? (String) from.get("username") : null;

        // Pattern /start <token>
        Pattern startPattern = Pattern.compile("^/start(?:@\\w+)?(?:\\s+(.+))?$", Pattern.CASE_INSENSITIVE);
        Matcher startMatcher = startPattern.matcher(text);

        if (startMatcher.matches()) {
            String token = startMatcher.group(1);
            if (token != null && !token.isBlank()) {
                String hashedToken = sha256(token.trim());
                return inviteRepositoryPort.findValidByToken(hashedToken)
                        .flatMap(invite -> {
                            invite.setIsUsed(true);
                            invite.setUpdatedAt(Instant.now());

                            TelegramSubscriber subscriber = TelegramSubscriber.builder()
                                    .companyId(invite.getCompanyId())
                                    .telegramChatId(chatId)
                                    .telegramUsername(username)
                                    .inviteToken(hashedToken)
                                    .isActive(true)
                                    .subscribedAt(Instant.now())
                                    .createdAt(Instant.now())
                                    .updatedAt(Instant.now())
                                    .build();

                            String welcome = "🎉 <b>Subscribed Successfully!</b>\nYou will now receive instant ticket issue alerts for your organization.";

                            return Mono.when(
                                    inviteRepositoryPort.save(invite),
                                    subscriberRepositoryPort.save(subscriber),
                                    telegramPort.sendMessage(chatId, welcome)
                            ).thenReturn(OK_RESPONSE);
                        })
                        .switchIfEmpty(telegramPort.sendMessage(chatId, "⚠️ <b>Invalid or Expired Invite:</b> Please request a new invite link from your admin.")
                                .thenReturn(OK_RESPONSE));
            } else {
                String greet = "👋 <b>Welcome!</b>\nTo subscribe to ticket notifications, please use the invite link provided by your organization.";
                return telegramPort.sendMessage(chatId, greet).thenReturn(OK_RESPONSE);
            }
        }

        // Pattern /status
        if (text.equalsIgnoreCase("/status")) {
            // Check if active
            return subscriberRepositoryPort.findByChatIdAndCompanyId(chatId, 1L)
                    .flatMap(sub -> {
                        String statusMsg = Boolean.TRUE.equals(sub.getIsActive())
                                ? String.format("✅ You are currently <b>subscribed</b> to ticket alerts for Company #%d.", sub.getCompanyId())
                                : "🔕 Your subscription is currently <b>muted</b>.";
                        return telegramPort.sendMessage(chatId, statusMsg);
                    })
                    .switchIfEmpty(telegramPort.sendMessage(chatId, "ℹ️ You are not currently subscribed to any organization ticket alerts."))
                    .thenReturn(OK_RESPONSE);
        }

        return Mono.just(OK_RESPONSE);
    }

    private String generateRandomToken(int length) {
        byte[] bytes = new byte[length / 2];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
