package Vibol.SEN.meetingManagements.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingMinutesRequest {
    private String agenda;
    private String summary;
    private String keyDecisions;
    private Long publishedById;
}
