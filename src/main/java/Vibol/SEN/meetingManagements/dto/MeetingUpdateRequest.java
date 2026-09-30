package Vibol.SEN.meetingManagements.dto;

import Vibol.SEN.meetingManagements.model.enums.MeetingStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingUpdateRequest {

    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    private String purpose;

    private Long roomId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private MeetingStatus status;

    private List<Long> attendeeIds;

    @Valid
    private List<MeetingMaterialRequest> materials;

    @Valid
    private List<MeetingStaffRequest> staffAssignments;
}
