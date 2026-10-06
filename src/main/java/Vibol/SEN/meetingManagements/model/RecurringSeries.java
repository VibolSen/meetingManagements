package Vibol.SEN.meetingManagements.model;

import Vibol.SEN.meetingManagements.model.enums.RecurrenceType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recurring_series")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringSeries {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "series_id")
    private Long seriesId;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type", nullable = false, length = 30)
    private RecurrenceType recurrenceType;

    @Column(name = "repeat_interval")
    @Builder.Default
    private Integer repeatInterval = 1;

    @Column(name = "days_of_week", length = 100)
    private String daysOfWeek; // e.g. "MONDAY,WEDNESDAY"

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "total_occurrences")
    private Integer totalOccurrences;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @OneToMany(mappedBy = "recurringSeries", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Meeting> meetings = new ArrayList<>();
}
