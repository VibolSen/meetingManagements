package Vibol.SEN.meetingManagements.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsSummaryResponse {
    private Long totalBookings;
    private Long completedBookings;
    private Long cancelledBookings;
    private Long autoReleasedNoShows;
    private Double totalMeetingHours;
    private Double overallFacilityUtilizationPct;
    private Double cancellationRatePct;
    private Double noShowRatePct;

    @Builder.Default
    private List<RoomUtilizationDTO> roomUtilizations = new ArrayList<>();

    @Builder.Default
    private List<DepartmentUsageDTO> departmentUsages = new ArrayList<>();

    @Builder.Default
    private List<HeatmapCellDTO> heatmap = new ArrayList<>();
}
