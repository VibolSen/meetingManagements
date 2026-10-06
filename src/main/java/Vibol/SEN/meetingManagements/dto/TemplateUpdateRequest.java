package Vibol.SEN.meetingManagements.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateUpdateRequest {

    @NotBlank(message = "Template content cannot be empty")
    private String content;
}
