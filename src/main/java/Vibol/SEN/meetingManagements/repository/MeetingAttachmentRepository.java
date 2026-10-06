package Vibol.SEN.meetingManagements.repository;

import Vibol.SEN.meetingManagements.model.MeetingAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MeetingAttachmentRepository extends JpaRepository<MeetingAttachment, Long> {
    List<MeetingAttachment> findByMeeting_MeetingId(Long meetingId);
}
