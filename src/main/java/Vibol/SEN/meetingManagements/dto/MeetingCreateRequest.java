package Vibol.SEN.meetingManagements.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingCreateRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    private String purpose;

    @NotNull(message = "Organizer ID is required")
    private Long organizerId;

    @NotNull(message = "Room ID is required")
    private Long roomId;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    @Builder.Default
    private List<Long> attendeeIds = new ArrayList<>();

    @Valid
    @Builder.Default
    private List<MeetingMaterialRequest> materials = new ArrayList<>();

    @Valid
    @Builder.Default
    private List<MeetingStaffRequest> staffAssignments = new ArrayList<>();
}
