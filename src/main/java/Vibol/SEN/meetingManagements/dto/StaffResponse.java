package Vibol.SEN.meetingManagements.dto;

import Vibol.SEN.meetingManagements.model.enums.StaffAvailability;
import Vibol.SEN.meetingManagements.model.enums.StaffRole;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffResponse {
    private Long staffId;
    private String name;
    private StaffRole role;
    private String skill;
    private StaffAvailability availabilityStatus;
}
