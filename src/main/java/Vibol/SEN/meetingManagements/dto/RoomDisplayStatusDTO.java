package Vibol.SEN.meetingManagements.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomDisplayStatusDTO {
    private RoomResponse room;
    private Boolean isOccupied;
    private MeetingResponse currentMeeting;
    private MeetingResponse nextMeeting;
    private Long secondsRemainingInCurrentMeeting;
    private Long minutesUntilNextMeeting;
    private LocalDateTime currentTime;

    @Builder.Default
    private List<MeetingResponse> todaySchedule = new ArrayList<>();

    @Builder.Default
    private List<RoomIssueDTO> activeIssues = new ArrayList<>();
}
