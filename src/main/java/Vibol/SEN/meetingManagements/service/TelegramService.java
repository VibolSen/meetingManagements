package Vibol.SEN.meetingManagements.service;

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

    @Value("${telegram.bot.token:8874617484:AAF2jAjzgGkxYla38mFlokCuKDS3bDpjxtY}")
    private String botToken;

    @Value("${telegram.bot.username:MMS_Meeting_Alert_Bot}")
    private String botUsername;

    @Value("${telegram.bot.enabled:true}")
    private Boolean botEnabled;

    @Value("${telegram.bot.default-chat-id:1035574371}")
    private String defaultChatId;

    private final RestClient restClient = RestClient.create();

    @Async
    public void sendMessage(String chatId, String message) {
        if (!isConfigured() || chatId == null || chatId.trim().isEmpty() || message == null || message.trim().isEmpty()) {
            log.debug("Telegram alert skipped (botEnabled={}, chatId={})", botEnabled, chatId);
            return;
        }

        try {
            String url = "https://api.telegram.org/bot" + botToken.trim() + "/sendMessage";
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
        String recipient = (targetChatId != null && !targetChatId.trim().isEmpty()) ? targetChatId.trim() : defaultChatId;
        if (!isConfigured() || recipient == null || recipient.isEmpty()) {
            return false;
        }

        String safeBotName = botUsername != null ? botUsername.replace("_", "\\_") : "MMS\\_Meeting\\_Alert\\_Bot";
        String testMessage = "🔔 *MMS TELEGRAM INTEGRATION TEST*\n\n" +
                "✅ *Status:* Successfully Connected\n" +
                "🤖 *Bot:* @" + safeBotName + "\n" +
                "📱 *Target Chat ID:* `" + recipient + "`\n\n" +
                "You will receive automated pre-meeting reminders and booking lifecycle alerts right here!";

        sendMessage(recipient, testMessage);
        return true;
    }

    public boolean isConfigured() {
        return Boolean.TRUE.equals(botEnabled) && botToken != null && !botToken.isBlank() && !botToken.contains("your_bot_token");
    }

    public String getBotUsername() {
        return botUsername;
    }

    public String getDefaultChatId() {
        return defaultChatId;
    }
}
