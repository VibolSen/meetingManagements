package Vibol.SEN.meetingManagements.controller;

import Vibol.SEN.meetingManagements.dto.*;
import Vibol.SEN.meetingManagements.model.NotificationTemplate;
import Vibol.SEN.meetingManagements.model.enums.AuditActionType;
import Vibol.SEN.meetingManagements.model.enums.AuditEntityType;
import Vibol.SEN.meetingManagements.model.enums.NotificationType;
import Vibol.SEN.meetingManagements.repository.NotificationTemplateRepository;
import Vibol.SEN.meetingManagements.service.AuditLogService;
import Vibol.SEN.meetingManagements.service.SystemSettingService;
import Vibol.SEN.meetingManagements.service.TelegramService;
import Vibol.SEN.meetingManagements.service.TemplateRenderService;
import Vibol.SEN.meetingManagements.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/telegram")
@RequiredArgsConstructor
@Slf4j
public class TelegramController {

    private final TelegramService telegramService;
    private final TemplateRenderService templateRenderService;
    private final NotificationTemplateRepository templateRepository;
    private final UserService userService;
    private final SystemSettingService systemSettingService;
    private final AuditLogService auditLogService;

    @GetMapping("/status")
    public ResponseEntity<TelegramStatusResponse> getStatus() {
        return ResponseEntity.ok(TelegramStatusResponse.builder()
                .botEnabled(telegramService.isBotEnabled())
                .botUsername(telegramService.getBotUsername())
                .defaultChatId(telegramService.getDefaultChatId())
                .defaultReminderMinutes(telegramService.getDefaultReminderMinutes())
                .botLink("https://t.me/" + telegramService.getBotUsername())
                .build());
    }

    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TelegramConfigResponse> getConfig() {
        String token = telegramService.getBotToken();
        boolean hasToken = token != null && !token.isBlank();
        String maskedToken = hasToken ? telegramService.maskToken(token) : "";

        return ResponseEntity.ok(TelegramConfigResponse.builder()
                .botEnabled(telegramService.isBotEnabled())
                .botTokenMasked(maskedToken)
                .hasToken(hasToken)
                .botUsername(telegramService.getBotUsername())
                .defaultChatId(telegramService.getDefaultChatId())
                .defaultReminderMinutes(telegramService.getDefaultReminderMinutes())
                .botLink("https://t.me/" + telegramService.getBotUsername())
                .build());
    }

    @PutMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TelegramConfigResponse> updateConfig(
            @Valid @RequestBody TelegramConfigUpdateRequest request,
            Authentication authentication
    ) {
        String author = (authentication != null && authentication.getName() != null)
                ? authentication.getName()
                : "ADMIN";

        if (request.getBotToken() != null && !request.getBotToken().isBlank() && !request.getBotToken().contains("••••")) {
            systemSettingService.updateSetting("notification.telegram_bot_token", request.getBotToken().trim(), author);
        }
        if (request.getBotUsername() != null && !request.getBotUsername().isBlank()) {
            systemSettingService.updateSetting("notification.telegram_bot_username", request.getBotUsername().trim().replace("@", ""), author);
        }
        if (request.getDefaultChatId() != null && !request.getDefaultChatId().isBlank()) {
            systemSettingService.updateSetting("notification.telegram_default_chat_id", request.getDefaultChatId().trim(), author);
        }
        if (request.getBotEnabled() != null) {
            systemSettingService.updateSetting("notification.telegram_enabled", String.valueOf(request.getBotEnabled()), author);
        }
        if (request.getDefaultReminderMinutes() != null) {
            systemSettingService.updateSetting("notification.default_lead_minutes", String.valueOf(request.getDefaultReminderMinutes()), author);
        }

        try {
            auditLogService.recordLog(
                    null,
                    author,
                    author + "@meetinghub.internal",
                    AuditActionType.UPDATE,
                    AuditEntityType.SYSTEM,
                    null,
                    "Telegram Bot Gateway Settings",
                    "Updated Telegram Bot Token, username, broadcast channel, or enabled status",
                    "127.0.0.1"
            );
        } catch (Exception auditEx) {
            log.warn("Failed to record audit log for Telegram config update: {}", auditEx.getMessage());
        }

        return getConfig();
    }

    @PostMapping("/validate-token")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TelegramValidationResponse> validateToken(
            @RequestBody(required = false) Map<String, String> request
    ) {
        String token = (request != null) ? request.get("token") : null;
        TelegramValidationResponse result = telegramService.validateToken(token);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> sendTestMessage(@RequestBody(required = false) TelegramTestRequest request) {
        String targetChatId = (request != null && request.getChatId() != null && !request.getChatId().isBlank())
                ? request.getChatId().trim()
                : telegramService.getDefaultChatId();

        if (request != null && request.getCustomMessage() != null && !request.getCustomMessage().isBlank()) {
            telegramService.sendMessage(targetChatId, request.getCustomMessage());
        } else {
            telegramService.sendTestAlert(targetChatId);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("targetChatId", targetChatId);
        resp.put("message", "Test alert dispatched to Telegram chat " + targetChatId);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/templates")
    public ResponseEntity<List<NotificationTemplateDTO>> getAllTemplates() {
        List<NotificationTemplateDTO> result = new ArrayList<>();

        for (NotificationType type : NotificationType.values()) {
            Optional<NotificationTemplate> existing = templateRepository.findByType(type);
            if (existing.isPresent()) {
                NotificationTemplate t = existing.get();
                result.add(NotificationTemplateDTO.builder()
                        .templateId(t.getTemplateId())
                        .type(t.getType())
                        .name(t.getName())
                        .content(t.getContent())
                        .isCustomized(t.getIsCustomized())
                        .updatedAt(t.getUpdatedAt())
                        .build());
            } else {
                result.add(NotificationTemplateDTO.builder()
                        .type(type)
                        .name(getFriendlyName(type))
                        .content(templateRenderService.getDefaultTemplate(type))
                        .isCustomized(false)
                        .updatedAt(LocalDateTime.now())
                        .build());
            }
        }

        return ResponseEntity.ok(result);
    }

    @PutMapping("/templates/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificationTemplateDTO> updateTemplate(
            @PathVariable NotificationType type,
            @Valid @RequestBody TemplateUpdateRequest request) {

        NotificationTemplate template = templateRepository.findByType(type)
                .orElseGet(() -> NotificationTemplate.builder()
                        .type(type)
                        .name(getFriendlyName(type))
                        .build());

        template.setContent(request.getContent().trim());
        template.setIsCustomized(true);
        NotificationTemplate saved = templateRepository.save(template);

        return ResponseEntity.ok(NotificationTemplateDTO.builder()
                .templateId(saved.getTemplateId())
                .type(saved.getType())
                .name(saved.getName())
                .content(saved.getContent())
                .isCustomized(saved.getIsCustomized())
                .updatedAt(saved.getUpdatedAt())
                .build());
    }

    @PostMapping("/templates/{type}/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificationTemplateDTO> resetTemplate(@PathVariable NotificationType type) {
        String defaultContent = templateRenderService.getDefaultTemplate(type);

        NotificationTemplate template = templateRepository.findByType(type)
                .orElseGet(() -> NotificationTemplate.builder()
                        .type(type)
                        .name(getFriendlyName(type))
                        .build());

        template.setContent(defaultContent);
        template.setIsCustomized(false);
        NotificationTemplate saved = templateRepository.save(template);

        return ResponseEntity.ok(NotificationTemplateDTO.builder()
                .templateId(saved.getTemplateId())
                .type(saved.getType())
                .name(saved.getName())
                .content(saved.getContent())
                .isCustomized(saved.getIsCustomized())
                .updatedAt(saved.getUpdatedAt())
                .build());
    }

    @PostMapping("/templates/preview")
    public ResponseEntity<Map<String, String>> previewTemplate(@RequestBody TemplatePreviewRequest request) {
        Map<String, String> sampleVars = new HashMap<>();
        sampleVars.put("title", "Executive Board Strategy Review");
        sampleVars.put("room", "Executive Boardroom (Building A, 4th Floor)");
        sampleVars.put("startTime", "Today • 02:00 PM");
        sampleVars.put("endTime", "03:30 PM");
        sampleVars.put("organizer", "Vibol SEN");
        sampleVars.put("purpose", "Quarterly operational sync & infrastructure roadmap");
        sampleVars.put("attendees", "Vibol SEN, Alice Johnson, Michael Chang");
        sampleVars.put("materials", "4K Laser Projector (1), Interactive Digital Whiteboard (1)");
        sampleVars.put("leadMinutes", "10");
        sampleVars.put("reason", "Rescheduled to accommodate executive availability");

        if (request.getSampleVariables() != null) {
            sampleVars.putAll(request.getSampleVariables());
        }

        String rawContent = request.getContent() != null && !request.getContent().isBlank()
                ? request.getContent()
                : (request.getType() != null ? templateRenderService.getDefaultTemplate(request.getType()) : "");

        String rendered = rawContent;
        for (Map.Entry<String, String> entry : sampleVars.entrySet()) {
            rendered = rendered.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        rendered = rendered.replaceAll("\\{[a-zA-Z0-9_]+\\}", "-");

        Map<String, String> response = new HashMap<>();
        response.put("rendered", rendered);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/users/{userId}/settings")
    public ResponseEntity<UserDTO> updateUserTelegramSettings(
            @PathVariable Long userId,
            @Valid @RequestBody TelegramSettingsRequest request) {
        UserDTO updated = userService.updateTelegramSettings(userId, request);
        return ResponseEntity.ok(updated);
    }

    private String getFriendlyName(NotificationType type) {
        switch (type) {
            case REMINDER:
                return "Pre-Meeting Countdown Reminder";
            case CONFIRMATION:
                return "Meeting Creation & Confirmation";
            case CHANGE:
                return "Meeting Approval & Modification";
            case CANCELLATION:
                return "Meeting Cancellation";
            default:
                return type.name();
        }
    }
}
