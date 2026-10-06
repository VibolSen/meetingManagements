package Vibol.SEN.meetingManagements.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_minutes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingMinutes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "minutes_id")
    private Long minutesId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "attendees", "materials", "staffAssignments"})
    private Meeting meeting;

    @Column(name = "agenda", columnDefinition = "TEXT")
    private String agenda;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "key_decisions", columnDefinition = "TEXT")
    private String keyDecisions;

    @CreationTimestamp
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password"})
    private User publishedBy;
}
