package Vibol.SEN.meetingManagements.dto;

import Vibol.SEN.meetingManagements.model.enums.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplateDTO {
    private Long templateId;
    private NotificationType type;
    private String name;
    private String content;
    private Boolean isCustomized;
    private LocalDateTime updatedAt;
}
