package Vibol.SEN.meetingManagements.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingMaterialRequest {

    @NotNull(message = "Material ID is required")
    private Long materialId;

    @NotNull(message = "Quantity requested is required")
    @Min(value = 1, message = "Quantity requested must be at least 1")
    private Integer quantityRequested;
}
