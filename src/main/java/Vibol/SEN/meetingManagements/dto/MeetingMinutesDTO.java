package Vibol.SEN.meetingManagements.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingMinutesDTO {
    private Long minutesId;
    private Long meetingId;
    private String agenda;
    private String summary;
    private String keyDecisions;
    private LocalDateTime publishedAt;
    private UserDTO publishedBy;
}
