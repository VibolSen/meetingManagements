package Vibol.SEN.meetingManagements.repository;

import Vibol.SEN.meetingManagements.model.MeetingMinutes;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MeetingMinutesRepository extends JpaRepository<MeetingMinutes, Long> {
    Optional<MeetingMinutes> findByMeeting_MeetingId(Long meetingId);
}
