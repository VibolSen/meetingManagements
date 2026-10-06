package Vibol.SEN.meetingManagements.dto;

import Vibol.SEN.meetingManagements.model.enums.SettingCategory;
import Vibol.SEN.meetingManagements.model.enums.SettingDataType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSettingResponse {
    private String settingKey;
    private String settingValue;
    private SettingCategory category;
    private SettingDataType dataType;
    private String displayName;
    private String description;
    private Boolean isPublic;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
