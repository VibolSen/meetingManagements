package Vibol.SEN.meetingManagements.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelegramConfigResponse {
    private boolean botEnabled;
    private String botTokenMasked;
    private boolean hasToken;
    private String botUsername;
    private String defaultChatId;
    private int defaultReminderMinutes;
    private String botLink;
}
