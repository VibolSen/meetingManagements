package Vibol.SEN.meetingManagements.dto;

import Vibol.SEN.meetingManagements.model.enums.RoomIssueStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomIssueUpdateRequest {

    @NotNull(message = "Status is required")
    private RoomIssueStatus status;

    private String resolutionNotes;
}
