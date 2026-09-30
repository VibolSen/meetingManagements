package Vibol.SEN.meetingManagements.dto;

import Vibol.SEN.meetingManagements.model.enums.MeetingStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingResponse {
    private Long meetingId;
    private String title;
    private String purpose;
    private MeetingStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private UserDTO organizer;
    private RoomResponse room;

    @Builder.Default
    private List<MeetingAttendeeResponse> attendees = new ArrayList<>();

    @Builder.Default
    private List<MeetingMaterialResponse> materials = new ArrayList<>();

    @Builder.Default
    private List<MeetingStaffResponse> staffAssignments = new ArrayList<>();
}
