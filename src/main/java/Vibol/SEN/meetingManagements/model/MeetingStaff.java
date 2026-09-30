package Vibol.SEN.meetingManagements.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "meeting_staff")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingStaff {

    @EmbeddedId
    @Builder.Default
    private MeetingStaffId id = new MeetingStaffId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("meetingId")
    @JoinColumn(name = "meeting_id")
    @JsonIgnore
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("staffId")
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @Size(max = 50, message = "Assigned role cannot exceed 50 characters")
    @Column(name = "assigned_role", length = 50)
    private String assignedRole;
}
