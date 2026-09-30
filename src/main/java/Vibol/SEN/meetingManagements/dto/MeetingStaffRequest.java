package Vibol.SEN.meetingManagements.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingStaffRequest {

    @NotNull(message = "Staff ID is required")
    private Long staffId;

    @Size(max = 50, message = "Assigned role cannot exceed 50 characters")
    private String assignedRole;
}
