package Vibol.SEN.meetingManagements.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MeetingStaffId implements Serializable {

    @Column(name = "meeting_id")
    private Long meetingId;

    @Column(name = "staff_id")
    private Long staffId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MeetingStaffId that = (MeetingStaffId) o;
        return Objects.equals(meetingId, that.meetingId) && Objects.equals(staffId, that.staffId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(meetingId, staffId);
    }
}
