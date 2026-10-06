package Vibol.SEN.meetingManagements.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingAttachmentCreateRequest {
    @NotBlank(message = "File name is required")
    private String fileName;

    private String fileType;

    @NotBlank(message = "File URL or content is required")
    private String fileUrl;

    private Long fileSize;

    private Long uploadedById;
}
