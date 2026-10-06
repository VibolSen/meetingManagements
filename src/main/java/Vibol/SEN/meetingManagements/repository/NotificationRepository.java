package Vibol.SEN.meetingManagements.repository;

import Vibol.SEN.meetingManagements.model.Notification;
import Vibol.SEN.meetingManagements.model.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import Vibol.SEN.meetingManagements.model.enums.NotificationType;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipient_UserIdOrderBySentAtDesc(Long userId);
    List<Notification> findByRecipient_UserIdAndStatusNot(Long userId, NotificationStatus status);
    List<Notification> findByStatus(NotificationStatus status);
    List<Notification> findByMeeting_MeetingId(Long meetingId);
    boolean existsByMeeting_MeetingIdAndRecipient_UserIdAndType(Long meetingId, Long userId, NotificationType type);
}
