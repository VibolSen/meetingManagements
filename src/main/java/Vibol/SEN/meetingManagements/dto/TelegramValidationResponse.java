package Vibol.SEN.meetingManagements.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelegramValidationResponse {
    private boolean valid;
    private Long botId;
    private String botName;
    private String username;
    private String errorMessage;
}
