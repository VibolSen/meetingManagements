package Vibol.SEN.meetingManagements.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentUsageDTO {
    private Long departmentId;
    private String departmentName;
    private Long totalMeetings;
    private Double totalHours;
    private Double percentage;
}
