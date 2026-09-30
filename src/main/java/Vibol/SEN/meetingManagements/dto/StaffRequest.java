package Vibol.SEN.meetingManagements.dto;

import Vibol.SEN.meetingManagements.model.enums.StaffAvailability;
import Vibol.SEN.meetingManagements.model.enums.StaffRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffRequest {

    @NotBlank(message = "Staff name is required")
    @Size(max = 150, message = "Staff name cannot exceed 150 characters")
    private String name;

    @NotNull(message = "Staff role is required")
    private StaffRole role;

    @Size(max = 150, message = "Skill cannot exceed 150 characters")
    private String skill;

    @NotNull(message = "Availability status is required")
    private StaffAvailability availabilityStatus;
}
