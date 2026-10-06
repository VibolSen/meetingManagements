package Vibol.SEN.meetingManagements.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeatmapCellDTO {
    private Integer dayOfWeek;
    private String dayName;
    private Integer hour;
    private Integer meetingCount;
    private Double intensity;
}
