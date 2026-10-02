package Vibol.SEN.meetingManagements.dto;

import Vibol.SEN.meetingManagements.model.enums.UserRole;
import Vibol.SEN.meetingManagements.model.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {

    @NotBlank(message = "User name is required")
    @Size(max = 150, message = "User name cannot exceed 150 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    private String email;

    @NotNull(message = "Role is required")
    private UserRole role;

    private UserStatus status;

    private String avatarUrl;

    private String password;

    private Long departmentId;
}
