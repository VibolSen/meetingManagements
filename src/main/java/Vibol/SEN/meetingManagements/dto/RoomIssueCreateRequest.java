package Vibol.SEN.meetingManagements.dto;

import Vibol.SEN.meetingManagements.model.enums.IssuePriority;
import Vibol.SEN.meetingManagements.model.enums.RoomIssueCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomIssueCreateRequest {

    private Long roomId;

    private Long reportedById;

    private String reporterName;

    @NotNull(message = "Category is required")
    private RoomIssueCategory category;

    @NotNull(message = "Priority is required")
    private IssuePriority priority;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    private String description;
}
