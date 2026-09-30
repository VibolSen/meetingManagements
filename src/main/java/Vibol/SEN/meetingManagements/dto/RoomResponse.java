package Vibol.SEN.meetingManagements.dto;

import Vibol.SEN.meetingManagements.model.enums.RoomStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponse {
    private Long roomId;
    private String name;
    private String location;
    private Integer capacity;
    private RoomStatus status;
}
