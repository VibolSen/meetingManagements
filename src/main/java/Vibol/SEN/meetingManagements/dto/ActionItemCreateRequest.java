package Vibol.SEN.meetingManagements.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionItemCreateRequest {
    private Long assigneeId;

    @NotBlank(message = "Task description is required")
    private String taskDescription;

    private LocalDate dueDate;
}
