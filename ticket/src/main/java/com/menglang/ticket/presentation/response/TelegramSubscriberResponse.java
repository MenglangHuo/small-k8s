package com.menglang.ticket.presentation.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.menglang.ticket.domain.model.TelegramSubscriber;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TelegramSubscriberResponse {
    private String id;
    private Long companyId;
    private String telegramChatId;
    private String telegramUsername;
    private String inviteToken;
    private Boolean isActive;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Phnom_Penh")
    private Instant subscribedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Phnom_Penh")
    private Instant createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Phnom_Penh")
    private Instant updatedAt;

    public static TelegramSubscriberResponse fromDomain(TelegramSubscriber sub) {
        if (sub == null) return null;
        return TelegramSubscriberResponse.builder()
                .id(sub.getId())
                .companyId(sub.getCompanyId())
                .telegramChatId(sub.getTelegramChatId())
                .telegramUsername(sub.getTelegramUsername())
                .inviteToken(sub.getInviteToken())
                .isActive(sub.getIsActive())
                .subscribedAt(sub.getSubscribedAt())
                .createdAt(sub.getCreatedAt())
                .updatedAt(sub.getUpdatedAt())
                .build();
    }
}
