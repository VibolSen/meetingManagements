package Vibol.SEN.meetingManagements.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingAttachmentDTO {
    private Long attachmentId;
    private Long meetingId;
    private String fileName;
    private String fileType;
    private String fileUrl;
    private Long fileSize;
    private LocalDateTime uploadedAt;
    private UserDTO uploadedBy;
}
