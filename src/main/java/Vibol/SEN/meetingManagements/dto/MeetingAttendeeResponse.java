package Vibol.SEN.meetingManagements.dto;

import Vibol.SEN.meetingManagements.model.enums.AttendeeResponseStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingAttendeeResponse {
    private Long userId;
    private String name;
    private String email;
    private AttendeeResponseStatus responseStatus;
}
