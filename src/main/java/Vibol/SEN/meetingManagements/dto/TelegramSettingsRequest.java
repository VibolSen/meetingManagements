package Vibol.SEN.meetingManagements.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelegramSettingsRequest {

    private String telegramChatId;

    private String telegramUsername;

    @Min(value = 1, message = "Reminder lead time must be at least 1 minute")
    @Max(value = 1440, message = "Reminder lead time cannot exceed 1440 minutes (24 hours)")
    private Integer telegramReminderMinutes;

    private Boolean telegramNotificationsEnabled;
}
