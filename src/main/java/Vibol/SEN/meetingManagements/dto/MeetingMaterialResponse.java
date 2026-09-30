package Vibol.SEN.meetingManagements.dto;

import Vibol.SEN.meetingManagements.model.enums.MaterialType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingMaterialResponse {
    private Long materialId;
    private String name;
    private MaterialType type;
    private Integer quantityRequested;
}
