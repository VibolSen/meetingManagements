package Vibol.SEN.meetingManagements.repository;

import Vibol.SEN.meetingManagements.model.Notification;
import Vibol.SEN.meetingManagements.model.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipient_UserIdOrderBySentAtDesc(Long userId);
    List<Notification> findByStatus(NotificationStatus status);
    List<Notification> findByMeeting_MeetingId(Long meetingId);
}
