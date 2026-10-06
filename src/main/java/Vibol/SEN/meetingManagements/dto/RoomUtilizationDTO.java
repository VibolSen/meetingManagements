package Vibol.SEN.meetingManagements.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomUtilizationDTO {
    private Long roomId;
    private String roomName;
    private Integer capacity;
    private Long totalMeetings;
    private Double totalHours;
    private Double utilizationPct;
}
