package Vibol.SEN.meetingManagements.repository;

import Vibol.SEN.meetingManagements.model.MeetingAttendee;
import Vibol.SEN.meetingManagements.model.MeetingAttendeeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MeetingAttendeeRepository extends JpaRepository<MeetingAttendee, MeetingAttendeeId> {
    List<MeetingAttendee> findById_MeetingId(Long meetingId);
    List<MeetingAttendee> findById_UserId(Long userId);
    Optional<MeetingAttendee> findById_MeetingIdAndId_UserId(Long meetingId, Long userId);
    void deleteById_MeetingId(Long meetingId);
}
