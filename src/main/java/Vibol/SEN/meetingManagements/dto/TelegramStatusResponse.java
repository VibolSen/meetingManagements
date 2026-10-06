package Vibol.SEN.meetingManagements.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelegramStatusResponse {
    private boolean botEnabled;
    private String botUsername;
    private String defaultChatId;
    private int defaultReminderMinutes;
    private String botLink;
}
