package com.menglang.ticket.infrastructure.telegram;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "telegram")
public class TelegramConfig {
    private String botToken = "";
    private String botUsername = "";
    private String defaultChatId = "";
    private String webhookSecret = "";
    private int inviteExpiryDays = 7;
}
