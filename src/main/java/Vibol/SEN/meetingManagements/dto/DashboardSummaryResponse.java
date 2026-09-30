package Vibol.SEN.meetingManagements.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryResponse {
    private long totalMeetings;
    private long pendingMeetings;
    private long confirmedMeetings;
    private long completedMeetings;
    private long cancelledMeetings;

    private long totalRooms;
    private long activeRooms;

    private long totalStaff;
    private long availableStaff;

    private long totalMaterials;

    @Builder.Default
    private List<MeetingResponse> upcomingMeetings = new ArrayList<>();
}
