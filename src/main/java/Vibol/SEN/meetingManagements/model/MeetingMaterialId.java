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
public class MeetingMaterialId implements Serializable {

    @Column(name = "meeting_id")
    private Long meetingId;

    @Column(name = "material_id")
    private Long materialId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MeetingMaterialId that = (MeetingMaterialId) o;
        return Objects.equals(meetingId, that.meetingId) && Objects.equals(materialId, that.materialId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(meetingId, materialId);
    }
}
