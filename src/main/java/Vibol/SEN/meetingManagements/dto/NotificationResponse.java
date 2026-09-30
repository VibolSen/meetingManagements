package Vibol.SEN.meetingManagements.dto;

import Vibol.SEN.meetingManagements.model.enums.NotificationStatus;
import Vibol.SEN.meetingManagements.model.enums.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long notificationId;
    private Long meetingId;
    private String meetingTitle;
    private Long recipientId;
    private String recipientName;
    private NotificationType type;
    private String message;
    private LocalDateTime sentAt;
    private NotificationStatus status;
}
