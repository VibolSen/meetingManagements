package Vibol.SEN.meetingManagements.dto;

import Vibol.SEN.meetingManagements.model.enums.BookingAccessLevel;
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
    private BookingAccessLevel bookingAccess;
    private String avatarUrl;
    private String phone;
    private String jobTitle;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
    private Long departmentId;
    private String departmentName;
    private String telegramChatId;
    private String telegramUsername;
    private Integer telegramReminderMinutes;
    private Boolean telegramNotificationsEnabled;
}
