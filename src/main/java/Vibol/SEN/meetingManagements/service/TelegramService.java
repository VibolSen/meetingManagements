package Vibol.SEN.meetingManagements.service;

import Vibol.SEN.meetingManagements.dto.TelegramValidationResponse;
import Vibol.SEN.meetingManagements.model.Meeting;
import Vibol.SEN.meetingManagements.model.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramService {

    private final TemplateRenderService templateRenderService;
    private final SystemSettingService systemSettingService;

    @Value("${telegram.bot.token:}")
    private String fallbackBotToken;

    @Value("${telegram.bot.username:MMS_Meeting_Alert_Bot}")
    private String fallbackBotUsername;

    @Value("${telegram.bot.enabled:true}")
    private Boolean fallbackBotEnabled;

    @Value("${telegram.bot.default-chat-id:1035574371}")
    private String fallbackDefaultChatId;

    @Value("${telegram.reminder.default-minutes:10}")
    private Integer fallbackDefaultReminderMinutes;

    private final RestClient restClient = RestClient.create();

    public String getBotToken() {
        String dbToken = systemSettingService.getString("notification.telegram_bot_token", null);
        if (dbToken != null && !dbToken.trim().isEmpty()) {
            return dbToken.trim();
        }
        String altToken = systemSettingService.getString("telegram.bot.token", null);
        if (altToken != null && !altToken.trim().isEmpty()) {
            return altToken.trim();
        }
        return fallbackBotToken != null ? fallbackBotToken.trim() : "";
    }

    public String getBotUsername() {
        String dbUsername = systemSettingService.getString("notification.telegram_bot_username", null);
        if (dbUsername != null && !dbUsername.trim().isEmpty()) {
            return dbUsername.trim();
        }
        String altUsername = systemSettingService.getString("telegram.bot.username", null);
        if (altUsername != null && !altUsername.trim().isEmpty()) {
            return altUsername.trim();
        }
        return (fallbackBotUsername != null && !fallbackBotUsername.isBlank())
                ? fallbackBotUsername.trim()
                : "MMS_Meeting_Alert_Bot";
    }

    public String getDefaultChatId() {
        String dbChatId = systemSettingService.getString("notification.telegram_default_chat_id", null);
        if (dbChatId != null && !dbChatId.trim().isEmpty()) {
            return dbChatId.trim();
        }
        String altChatId = systemSettingService.getString("telegram.bot.default-chat-id", null);
        if (altChatId != null && !altChatId.trim().isEmpty()) {
            return altChatId.trim();
        }
        return (fallbackDefaultChatId != null && !fallbackDefaultChatId.isBlank())
                ? fallbackDefaultChatId.trim()
                : "1035574371";
    }

    public boolean isBotEnabled() {
        return systemSettingService.getBoolean(
                "notification.telegram_enabled",
                fallbackBotEnabled != null ? fallbackBotEnabled : true
        );
    }

    public int getDefaultReminderMinutes() {
        return systemSettingService.getInt(
                "notification.default_lead_minutes",
                fallbackDefaultReminderMinutes != null ? fallbackDefaultReminderMinutes : 10
        );
    }

    public boolean isConfigured() {
        String token = getBotToken();
        return isBotEnabled() && !token.isBlank() && !token.contains("your_bot_token");
    }

    public String maskToken(String token) {
        if (token == null || token.isBlank()) return "";
        String trimmed = token.trim();
        if (trimmed.length() <= 8) return "••••••••";
        int prefixLen = Math.min(6, trimmed.length() / 4);
        int suffixLen = Math.min(4, trimmed.length() / 4);
        return trimmed.substring(0, prefixLen) + "••••••••" + trimmed.substring(trimmed.length() - suffixLen);
    }

    public TelegramValidationResponse validateToken(String rawToken) {
        String tokenToTest = (rawToken != null && !rawToken.isBlank() && !rawToken.contains("•••"))
                ? rawToken.trim()
                : getBotToken();

        if (tokenToTest == null || tokenToTest.isBlank()) {
            return TelegramValidationResponse.builder()
                    .valid(false)
                    .errorMessage("No Telegram Bot Token provided or configured.")
                    .build();
        }

        try {
            String url = "https://api.telegram.org/bot" + tokenToTest + "/getMe";
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .body(Map.class);

            if (response != null && Boolean.TRUE.equals(response.get("ok"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) response.get("result");
                Long botId = null;
                if (result != null && result.get("id") instanceof Number) {
                    botId = ((Number) result.get("id")).longValue();
                }
                String botName = result != null ? (String) result.get("first_name") : null;
                String username = result != null ? (String) result.get("username") : null;

                log.info("Live Telegram token verification successful: @{} (ID: {})", username, botId);
                return TelegramValidationResponse.builder()
                        .valid(true)
                        .botId(botId)
                        .botName(botName)
                        .username(username)
                        .build();
            } else {
                String errorDesc = response != null && response.get("description") != null
                        ? response.get("description").toString()
                        : "Telegram API rejected token";
                return TelegramValidationResponse.builder()
                        .valid(false)
                        .errorMessage(errorDesc)
                        .build();
            }
        } catch (Exception e) {
            log.warn("Telegram token validation request failed: {}", e.getMessage());
            String message = e.getMessage() != null ? e.getMessage() : "Unable to reach Telegram API";
            // Clean up common Spring RestClient error output for UI readability
            if (message.contains("401") || message.contains("Unauthorized")) {
                message = "Invalid Bot Token (HTTP 401 Unauthorized)";
            } else if (message.contains("404") || message.contains("Not Found")) {
                message = "Invalid Bot Token endpoint (HTTP 404 Not Found)";
            }
            return TelegramValidationResponse.builder()
                    .valid(false)
                    .errorMessage(message)
                    .build();
        }
    }

    @Async
    public void sendMessage(String chatId, String message) {
        if (!isConfigured() || chatId == null || chatId.trim().isEmpty() || message == null || message.trim().isEmpty()) {
            log.debug("Telegram alert skipped (botEnabled={}, chatId={})", isBotEnabled(), chatId);
            return;
        }

        String token = getBotToken();
        try {
            String url = "https://api.telegram.org/bot" + token + "/sendMessage";
            Map<String, Object> payload = new HashMap<>();
            payload.put("chat_id", chatId.trim());
            payload.put("text", message);
            payload.put("parse_mode", "Markdown");

            try {
                restClient.post()
                        .uri(URI.create(url))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity();
            } catch (Exception parseEx) {
                // If Markdown parsing fails due to user symbols, fallback to plain text delivery
                log.warn("Markdown parse failed for chat [{}], falling back to plain text delivery: {}", chatId, parseEx.getMessage());
                payload.remove("parse_mode");
                restClient.post()
                        .uri(URI.create(url))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity();
            }

            log.info("Telegram notification successfully delivered to chat [{}]", chatId);
        } catch (Exception e) {
            log.warn("Failed to dispatch Telegram message to chat [{}]: {}", chatId, e.getMessage());
        }
    }

    public boolean sendDirectNotification(String chatId, NotificationType type, Meeting meeting, Integer leadMinutes, String reason) {
        if (!isConfigured() || chatId == null || chatId.trim().isEmpty()) {
            return false;
        }
        Map<String, String> variables = templateRenderService.buildVariables(meeting, leadMinutes, reason);
        String text = templateRenderService.renderTemplate(type, variables);
        sendMessage(chatId, text);
        return true;
    }

    public boolean sendTestAlert(String targetChatId) {
        String recipient = (targetChatId != null && !targetChatId.trim().isEmpty())
                ? targetChatId.trim()
                : getDefaultChatId();
        if (!isConfigured() || recipient == null || recipient.isEmpty()) {
            return false;
        }

        String username = getBotUsername();
        String safeBotName = username != null ? username.replace("_", "\\_") : "MMS\\_Meeting\\_Alert\\_Bot";
        String testMessage = "🔔 *MMS TELEGRAM INTEGRATION TEST*\n\n" +
                "✅ *Status:* Successfully Connected\n" +
                "🤖 *Bot:* @" + safeBotName + "\n" +
                "📱 *Target Chat ID:* `" + recipient + "`\n\n" +
                "You will receive automated pre-meeting reminders and booking lifecycle alerts right here!";

        sendMessage(recipient, testMessage);
        return true;
    }
}
