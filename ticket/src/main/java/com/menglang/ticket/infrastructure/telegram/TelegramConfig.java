package com.menglang.ticket.infrastructure.telegram;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "telegram")
public class TelegramConfig {
    private String botToken = "5453551747:AAHwNAd3BjrbqE5gLrK9kbAPQOp22Zmcfs4";
    private String botUsername = "menglang6969Bot";
    private String defaultChatId = "-1004247055582";
    private String webhookSecret = "";
    private int inviteExpiryDays = 7;
}
