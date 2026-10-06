package Vibol.SEN.meetingManagements.dto;

import Vibol.SEN.meetingManagements.model.enums.NotificationType;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplatePreviewRequest {
    private NotificationType type;
    private String content;
    private Map<String, String> sampleVariables;
}
