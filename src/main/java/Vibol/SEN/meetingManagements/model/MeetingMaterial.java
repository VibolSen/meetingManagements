package Vibol.SEN.meetingManagements.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "meeting_materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingMaterial {

    @EmbeddedId
    @Builder.Default
    private MeetingMaterialId id = new MeetingMaterialId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("meetingId")
    @JoinColumn(name = "meeting_id")
    @JsonIgnore
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("materialId")
    @JoinColumn(name = "material_id")
    private Material material;

    @NotNull(message = "Quantity requested is required")
    @Min(value = 1, message = "Quantity requested must be at least 1")
    @Column(name = "quantity_requested", nullable = false)
    private Integer quantityRequested;
}
