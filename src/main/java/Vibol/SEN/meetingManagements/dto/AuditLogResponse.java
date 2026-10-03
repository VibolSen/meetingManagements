package Vibol.SEN.meetingManagements.dto;

import Vibol.SEN.meetingManagements.model.enums.AuditActionType;
import Vibol.SEN.meetingManagements.model.enums.AuditEntityType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {
    private Long logId;
    private Long actorId;
    private String actorName;
    private String actorEmail;
    private AuditActionType actionType;
    private AuditEntityType entityType;
    private Long entityId;
    private String entityName;
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;
}
