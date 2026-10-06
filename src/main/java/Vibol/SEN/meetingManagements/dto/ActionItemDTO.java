package Vibol.SEN.meetingManagements.dto;

import Vibol.SEN.meetingManagements.model.enums.ActionItemStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionItemDTO {
    private Long itemId;
    private Long meetingId;
    private String meetingTitle;
    private UserDTO assignee;
    private String taskDescription;
    private LocalDate dueDate;
    private ActionItemStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
