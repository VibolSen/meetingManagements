package Vibol.SEN.meetingManagements.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelegramTestRequest {
    private String chatId;
    private String customMessage;
}
