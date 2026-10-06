package Vibol.SEN.meetingManagements.model;

import Vibol.SEN.meetingManagements.model.enums.StaffAvailability;
import Vibol.SEN.meetingManagements.model.enums.StaffRole;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "staff")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "staff_id")
    private Long staffId;

    @NotBlank(message = "Staff name is required")
    @Size(max = 150, message = "Staff name cannot exceed 150 characters")
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @NotNull(message = "Staff role is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private StaffRole role;

    @Size(max = 150, message = "Skill cannot exceed 150 characters")
    @Column(name = "skill", length = 150)
    private String skill;

    @NotNull(message = "Availability status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 30)
    @Builder.Default
    private StaffAvailability availabilityStatus = StaffAvailability.AVAILABLE;

    @JsonIgnore
    @OneToMany(mappedBy = "staff", fetch = FetchType.LAZY)
    @Builder.Default
    private List<MeetingStaff> meetingStaffList = new ArrayList<>();
}
