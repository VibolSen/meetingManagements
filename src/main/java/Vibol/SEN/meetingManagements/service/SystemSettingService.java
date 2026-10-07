package Vibol.SEN.meetingManagements.service;

import Vibol.SEN.meetingManagements.dto.SystemSettingResponse;
import Vibol.SEN.meetingManagements.dto.SystemSettingUpdateRequest;
import Vibol.SEN.meetingManagements.model.SystemSetting;
import Vibol.SEN.meetingManagements.model.enums.SettingCategory;
import Vibol.SEN.meetingManagements.model.enums.SettingDataType;
import Vibol.SEN.meetingManagements.repository.SystemSettingRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;
    private final Map<String, SystemSetting> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        initDefaultSettingsIfEmpty();
        ensureTelegramSettingsExist();
        reloadCache();
    }

    public synchronized void reloadCache() {
        cache.clear();
        systemSettingRepository.findAll().forEach(s -> cache.put(s.getSettingKey(), s));
        log.info("System settings cache loaded with {} configuration entries.", cache.size());
    }

    public String getString(String key, String defaultValue) {
        SystemSetting s = cache.get(key);
        return (s != null && s.getSettingValue() != null) ? s.getSettingValue() : defaultValue;
    }

    public String getSettingValue(String key) {
        SystemSetting s = cache.get(key);
        return (s != null && s.getSettingValue() != null) ? s.getSettingValue() : null;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        SystemSetting s = cache.get(key);
        if (s == null || s.getSettingValue() == null) return defaultValue;
        return Boolean.parseBoolean(s.getSettingValue().trim());
    }

    public int getInt(String key, int defaultValue) {
        SystemSetting s = cache.get(key);
        if (s == null || s.getSettingValue() == null) return defaultValue;
        try {
            return Integer.parseInt(s.getSettingValue().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Transactional(readOnly = true)
    public List<SystemSettingResponse> getAllSettings() {
        return systemSettingRepository.findAll().stream()
                .map(this::mapToResponse)
                .sorted((a, b) -> {
                    int cat = (a.getCategory() != null ? a.getCategory().name() : "")
                            .compareTo(b.getCategory() != null ? b.getCategory().name() : "");
                    if (cat != 0) return cat;
                    return (a.getSettingKey() != null ? a.getSettingKey() : "")
                            .compareTo(b.getSettingKey() != null ? b.getSettingKey() : "");
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SystemSettingResponse> getPublicSettings() {
        return systemSettingRepository.findByIsPublicTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SystemSettingResponse> getSettingsByCategory(SettingCategory category) {
        return systemSettingRepository.findByCategory(category).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public SystemSettingResponse updateSetting(String key, String value, String updatedBy) {
        SystemSetting setting = systemSettingRepository.findById(key)
                .orElseGet(() -> {
                    log.info("System setting [{}] not found; initializing new configuration entry.", key);
                    return SystemSetting.builder()
                            .settingKey(key)
                            .category(SettingCategory.NOTIFICATIONS)
                            .dataType(SettingDataType.STRING)
                            .displayName(key)
                            .isPublic(false)
                            .build();
                });

        setting.setSettingValue(value != null ? value : "");
        setting.setUpdatedBy(updatedBy != null ? updatedBy : "ADMIN");
        SystemSetting saved = systemSettingRepository.save(setting);
        cache.put(saved.getSettingKey(), saved);
        log.info("System setting [{}] updated to: '{}' by {}", key, value, updatedBy);
        return mapToResponse(saved);
    }

    public List<SystemSettingResponse> batchUpdate(List<SystemSettingUpdateRequest> updates, String updatedBy) {
        List<SystemSettingResponse> results = new ArrayList<>();
        for (SystemSettingUpdateRequest update : updates) {
            systemSettingRepository.findById(update.getSettingKey()).ifPresent(setting -> {
                setting.setSettingValue(update.getSettingValue());
                setting.setUpdatedBy(updatedBy != null ? updatedBy : "ADMIN");
                SystemSetting saved = systemSettingRepository.save(setting);
                cache.put(saved.getSettingKey(), saved);
                results.add(mapToResponse(saved));
            });
        }
        log.info("Batch updated {} system settings by {}", results.size(), updatedBy);
        return results;
    }

    public void resetToDefaults(String updatedBy) {
        systemSettingRepository.deleteAll();
        cache.clear();
        seedDefaultSettings(updatedBy);
        reloadCache();
        log.info("System settings reset to factory defaults by {}", updatedBy);
    }

    public void initDefaultSettingsIfEmpty() {
        if (systemSettingRepository.count() == 0) {
            seedDefaultSettings("SYSTEM_INIT");
        }
    }

    public void ensureTelegramSettingsExist() {
        ensureSetting("notification.telegram_enabled", "true", SettingDataType.BOOLEAN,
                "Enable Telegram Bot Notifications", "Global toggle for multi-channel Telegram automated alerts.", true);
        ensureSetting("notification.telegram_bot_token", "", SettingDataType.STRING,
                "Telegram Bot Token", "Telegram Bot Token obtained from @BotFather for dispatching automated alerts.", false);
        ensureSetting("notification.telegram_bot_username", "MMS_Meeting_Alert_Bot", SettingDataType.STRING,
                "Telegram Bot Username", "Telegram Bot Username (without @) for invitations and deep links.", true);
        ensureSetting("notification.telegram_default_chat_id", "1035574371", SettingDataType.STRING,
                "Telegram Default Broadcast Chat ID", "Default Telegram chat ID or channel ID for broadcast notifications.", true);
        ensureSetting("notification.default_lead_minutes", "10", SettingDataType.NUMBER,
                "Default Reminder Lead Time (Minutes)", "Default time prior to meeting start when reminder alerts are dispatched.", true);
    }

    private void ensureSetting(String key, String defaultValue, SettingDataType type, String displayName, String description, boolean isPublic) {
        if (!systemSettingRepository.existsById(key)) {
            SystemSetting s = SystemSetting.builder()
                    .settingKey(key)
                    .settingValue(defaultValue)
                    .category(SettingCategory.NOTIFICATIONS)
                    .dataType(type)
                    .displayName(displayName)
                    .description(description)
                    .isPublic(isPublic)
                    .updatedBy("SYSTEM_INIT")
                    .build();
            systemSettingRepository.save(s);
            log.info("Initialized missing notification setting [{}]", key);
        }
    }

    private void seedDefaultSettings(String author) {
        List<SystemSetting> defaults = List.of(
                // Role Permissions Matrix
                SystemSetting.builder()
                        .settingKey("role.employee.can_book")
                        .settingValue("true")
                        .category(SettingCategory.ROLE_PERMISSIONS)
                        .dataType(SettingDataType.BOOLEAN)
                        .displayName("Allow Employees to Book Meetings")
                        .description("Enables regular employees to submit meeting reservations directly.")
                        .isPublic(true)
                        .updatedBy(author)
                        .build(),
                SystemSetting.builder()
                        .settingKey("role.employee.require_approval")
                        .settingValue("false")
                        .category(SettingCategory.ROLE_PERMISSIONS)
                        .dataType(SettingDataType.BOOLEAN)
                        .displayName("Require Admin Approval for Employee Bookings")
                        .description("If enabled, all employee reservations remain PENDING until approved by an administrator.")
                        .isPublic(true)
                        .updatedBy(author)
                        .build(),
                SystemSetting.builder()
                        .settingKey("role.organizer.require_approval")
                        .settingValue("false")
                        .category(SettingCategory.ROLE_PERMISSIONS)
                        .dataType(SettingDataType.BOOLEAN)
                        .displayName("Require Admin Approval for Organizer Bookings")
                        .description("If enabled, organizer reservations require admin sign-off.")
                        .isPublic(false)
                        .updatedBy(author)
                        .build(),
                SystemSetting.builder()
                        .settingKey("role.employee.can_invite_external")
                        .settingValue("true")
                        .category(SettingCategory.ROLE_PERMISSIONS)
                        .dataType(SettingDataType.BOOLEAN)
                        .displayName("Allow Employees to Invite Cross-Department Staff")
                        .description("Allows employee bookers to invite members outside their own department.")
                        .isPublic(true)
                        .updatedBy(author)
                        .build(),
                SystemSetting.builder()
                        .settingKey("role.employee.can_request_materials")
                        .settingValue("true")
                        .category(SettingCategory.ROLE_PERMISSIONS)
                        .dataType(SettingDataType.BOOLEAN)
                        .displayName("Allow Employees to Request Equipment & Materials")
                        .description("Allows employees to request projectors, clickers, whiteboards, etc.")
                        .isPublic(true)
                        .updatedBy(author)
                        .build(),

                // Booking Policies
                SystemSetting.builder()
                        .settingKey("booking.attendee_acceptance_mode")
                        .settingValue("AUTO_ACCEPT")
                        .category(SettingCategory.BOOKING_POLICY)
                        .dataType(SettingDataType.STRING)
                        .displayName("Attendee Invitation Acceptance Mode")
                        .description("AUTO_ACCEPT: attendees are automatically accepted without waiting; RSVP_REQUIRED: attendees start in PENDING.")
                        .isPublic(true)
                        .updatedBy(author)
                        .build(),
                SystemSetting.builder()
                        .settingKey("booking.approval_threshold_capacity")
                        .settingValue("20")
                        .category(SettingCategory.BOOKING_POLICY)
                        .dataType(SettingDataType.NUMBER)
                        .displayName("Boardroom Capacity Approval Threshold")
                        .description("Meetings reserved in rooms with capacity equal or greater require mandatory Admin approval.")
                        .isPublic(true)
                        .updatedBy(author)
                        .build(),
                SystemSetting.builder()
                        .settingKey("booking.allow_weekend_booking")
                        .settingValue("false")
                        .category(SettingCategory.BOOKING_POLICY)
                        .dataType(SettingDataType.BOOLEAN)
                        .displayName("Allow Weekend Reservations")
                        .description("Permits reserving meeting rooms on Saturday and Sunday.")
                        .isPublic(true)
                        .updatedBy(author)
                        .build(),
                SystemSetting.builder()
                        .settingKey("booking.operating_hours_start")
                        .settingValue("08:00")
                        .category(SettingCategory.BOOKING_POLICY)
                        .dataType(SettingDataType.STRING)
                        .displayName("Facility Standard Opening Time")
                        .description("Start of allowed booking hours in 24-hour HH:mm format.")
                        .isPublic(true)
                        .updatedBy(author)
                        .build(),
                SystemSetting.builder()
                        .settingKey("booking.operating_hours_end")
                        .settingValue("18:00")
                        .category(SettingCategory.BOOKING_POLICY)
                        .dataType(SettingDataType.STRING)
                        .displayName("Facility Standard Closing Time")
                        .description("End of allowed booking hours in 24-hour HH:mm format.")
                        .isPublic(true)
                        .updatedBy(author)
                        .build(),
                SystemSetting.builder()
                        .settingKey("booking.max_advance_days")
                        .settingValue("60")
                        .category(SettingCategory.BOOKING_POLICY)
                        .dataType(SettingDataType.NUMBER)
                        .displayName("Maximum Advance Booking Horizon (Days)")
                        .description("Prevents scheduling reservations beyond this number of days into the future.")
                        .isPublic(true)
                        .updatedBy(author)
                        .build(),

                // Scheduling Defaults
                SystemSetting.builder()
                        .settingKey("scheduling.default_duration_min")
                        .settingValue("30")
                        .category(SettingCategory.SCHEDULING_DEFAULTS)
                        .dataType(SettingDataType.NUMBER)
                        .displayName("Default Meeting Duration (Minutes)")
                        .description("Initial meeting duration pre-selected in reservation dialogs.")
                        .isPublic(true)
                        .updatedBy(author)
                        .build(),
                SystemSetting.builder()
                        .settingKey("scheduling.room_buffer_min")
                        .settingValue("5")
                        .category(SettingCategory.SCHEDULING_DEFAULTS)
                        .dataType(SettingDataType.NUMBER)
                        .displayName("Room Turnover Buffer (Minutes)")
                        .description("Turnover sanitation and AV reset buffer required between consecutive reservations.")
                        .isPublic(true)
                        .updatedBy(author)
                        .build(),

                // Multi-Channel Notifications
                SystemSetting.builder()
                        .settingKey("notification.telegram_enabled")
                        .settingValue("true")
                        .category(SettingCategory.NOTIFICATIONS)
                        .dataType(SettingDataType.BOOLEAN)
                        .displayName("Enable Telegram Bot Notifications")
                        .description("Global toggle for multi-channel Telegram automated alerts.")
                        .isPublic(true)
                        .updatedBy(author)
                        .build(),
                SystemSetting.builder()
                        .settingKey("notification.telegram_bot_token")
                        .settingValue("")
                        .category(SettingCategory.NOTIFICATIONS)
                        .dataType(SettingDataType.STRING)
                        .displayName("Telegram Bot Token")
                        .description("Telegram Bot Token obtained from @BotFather for dispatching automated alerts.")
                        .isPublic(false)
                        .updatedBy(author)
                        .build(),
                SystemSetting.builder()
                        .settingKey("notification.telegram_bot_username")
                        .settingValue("MMS_Meeting_Alert_Bot")
                        .category(SettingCategory.NOTIFICATIONS)
                        .dataType(SettingDataType.STRING)
                        .displayName("Telegram Bot Username")
                        .description("Telegram Bot Username (without @) for invitations and deep links.")
                        .isPublic(true)
                        .updatedBy(author)
                        .build(),
                SystemSetting.builder()
                        .settingKey("notification.telegram_default_chat_id")
                        .settingValue("1035574371")
                        .category(SettingCategory.NOTIFICATIONS)
                        .dataType(SettingDataType.STRING)
                        .displayName("Telegram Default Broadcast Chat ID")
                        .description("Default Telegram chat ID or channel ID for broadcast notifications.")
                        .isPublic(true)
                        .updatedBy(author)
                        .build(),
                SystemSetting.builder()
                        .settingKey("notification.default_lead_minutes")
                        .settingValue("10")
                        .category(SettingCategory.NOTIFICATIONS)
                        .dataType(SettingDataType.NUMBER)
                        .displayName("Default Reminder Lead Time (Minutes)")
                        .description("Default time prior to meeting start when reminder alerts are dispatched.")
                        .isPublic(true)
                        .updatedBy(author)
                        .build(),

                // Branding & Identity
                SystemSetting.builder()
                        .settingKey("branding.app_name")
                        .settingValue("Meeting Management System")
                        .category(SettingCategory.BRANDING)
                        .dataType(SettingDataType.STRING)
                        .displayName("System Application Name")
                        .description("Application title displayed in navigation headers and browser title.")
                        .isPublic(true)
                        .updatedBy(author)
                        .build(),
                SystemSetting.builder()
                        .settingKey("branding.organization_name")
                        .settingValue("Enterprise Workspace")
                        .category(SettingCategory.BRANDING)
                        .dataType(SettingDataType.STRING)
                        .displayName("Organization / Company Name")
                        .description("Name of the hosting enterprise or company.")
                        .isPublic(true)
                        .updatedBy(author)
                        .build(),
                SystemSetting.builder()
                        .settingKey("branding.support_email")
                        .settingValue("support@workspace.internal")
                        .category(SettingCategory.BRANDING)
                        .dataType(SettingDataType.STRING)
                        .displayName("IT Helpdesk / Support Contact Email")
                        .description("Email address shown for booking assistance and escalation.")
                        .isPublic(true)
                        .updatedBy(author)
                        .build(),
                SystemSetting.builder()
                        .settingKey("branding.logo_url")
                        .settingValue("/default logo/meeting-time.svg")
                        .category(SettingCategory.BRANDING)
                        .dataType(SettingDataType.STRING)
                        .displayName("Application Header Logo Path")
                        .description("Relative or absolute URL path to the workspace logo image.")
                        .isPublic(true)
                        .updatedBy(author)
                        .build()
        );

        systemSettingRepository.saveAll(defaults);
        log.info("Initialized {} default system settings.", defaults.size());
    }

    private SystemSettingResponse mapToResponse(SystemSetting s) {
        return SystemSettingResponse.builder()
                .settingKey(s.getSettingKey())
                .settingValue(s.getSettingValue())
                .category(s.getCategory())
                .dataType(s.getDataType())
                .displayName(s.getDisplayName())
                .description(s.getDescription())
                .isPublic(s.getIsPublic())
                .updatedAt(s.getUpdatedAt())
                .updatedBy(s.getUpdatedBy())
                .build();
    }
}
