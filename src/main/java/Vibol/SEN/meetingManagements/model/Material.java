package Vibol.SEN.meetingManagements.model;

import Vibol.SEN.meetingManagements.model.enums.MaterialType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "material_id")
    private Long materialId;

    @NotBlank(message = "Material name is required")
    @Size(max = 150, message = "Material name cannot exceed 150 characters")
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @NotNull(message = "Material type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private MaterialType type;

    @NotNull(message = "Quantity available is required")
    @Min(value = 0, message = "Quantity available cannot be negative")
    @Column(name = "quantity_available", nullable = false)
    private Integer quantityAvailable;

    @JsonIgnore
    @OneToMany(mappedBy = "material", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<MeetingMaterial> meetingMaterials = new ArrayList<>();
}
