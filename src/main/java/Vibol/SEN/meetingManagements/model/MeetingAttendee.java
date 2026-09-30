package Vibol.SEN.meetingManagements.model;

import Vibol.SEN.meetingManagements.model.enums.AttendeeResponseStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meeting_attendees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingAttendee {

    @EmbeddedId
    @Builder.Default
    private MeetingAttendeeId id = new MeetingAttendeeId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("meetingId")
    @JoinColumn(name = "meeting_id")
    @JsonIgnore
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "response_status", length = 30)
    @Builder.Default
    private AttendeeResponseStatus responseStatus = AttendeeResponseStatus.PENDING;
}
