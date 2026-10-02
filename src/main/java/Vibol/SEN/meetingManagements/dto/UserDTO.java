package Vibol.SEN.meetingManagements.dto;

import Vibol.SEN.meetingManagements.model.enums.UserRole;
import Vibol.SEN.meetingManagements.model.enums.UserStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long userId;
    private String name;
    private String email;
    private UserRole role;
    private UserStatus status;
    private String avatarUrl;
    private Long departmentId;
    private String departmentName;
}
