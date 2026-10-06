package Vibol.SEN.meetingManagements.dto;

import Vibol.SEN.meetingManagements.model.enums.IssuePriority;
import Vibol.SEN.meetingManagements.model.enums.RoomIssueCategory;
import Vibol.SEN.meetingManagements.model.enums.RoomIssueStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomIssueDTO {
    private Long issueId;
    private Long roomId;
    private String roomName;
    private Long reportedById;
    private String reportedByName;
    private String reporterName;
    private RoomIssueCategory category;
    private IssuePriority priority;
    private String title;
    private String description;
    private RoomIssueStatus status;
    private String resolutionNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
}
