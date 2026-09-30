package Vibol.SEN.meetingManagements.dto;

import Vibol.SEN.meetingManagements.model.enums.StaffRole;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingStaffResponse {
    private Long staffId;
    private String name;
    private StaffRole role;
    private String assignedRole;
}
