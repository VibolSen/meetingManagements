package Vibol.SEN.meetingManagements.dto;

import Vibol.SEN.meetingManagements.model.enums.MaterialType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialRequest {

    @NotBlank(message = "Material name is required")
    @Size(max = 150, message = "Material name cannot exceed 150 characters")
    private String name;

    @NotNull(message = "Material type is required")
    private MaterialType type;

    @NotNull(message = "Quantity available is required")
    @Min(value = 0, message = "Quantity available cannot be negative")
    private Integer quantityAvailable;
}
