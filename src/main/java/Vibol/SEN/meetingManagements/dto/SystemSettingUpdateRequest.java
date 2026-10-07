package Vibol.SEN.meetingManagements.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSettingUpdateRequest {
    @NotBlank(message = "Setting key is required")
    private String settingKey;

    @NotNull(message = "Setting value is required")
    private String settingValue;
}
