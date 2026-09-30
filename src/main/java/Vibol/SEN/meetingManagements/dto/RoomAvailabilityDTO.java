package Vibol.SEN.meetingManagements.dto;

import Vibol.SEN.meetingManagements.model.enums.RoomStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomAvailabilityDTO {
    private Long roomId;
    private String name;
    private String location;
    private Integer capacity;
    private RoomStatus status;
    private boolean available;
    private String conflictReason;
    private LocalDateTime nextAvailableTime;
}
