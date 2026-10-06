package Vibol.SEN.meetingManagements.service;

import Vibol.SEN.meetingManagements.dto.NotificationResponse;
import Vibol.SEN.meetingManagements.exception.ResourceNotFoundException;
import Vibol.SEN.meetingManagements.model.Meeting;
import Vibol.SEN.meetingManagements.model.Notification;
import Vibol.SEN.meetingManagements.model.User;
import Vibol.SEN.meetingManagements.model.enums.NotificationStatus;
import Vibol.SEN.meetingManagements.model.enums.NotificationType;
import Vibol.SEN.meetingManagements.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final TelegramService telegramService;

    public NotificationResponse createNotification(Meeting meeting, User recipient, NotificationType type, String message) {
        Notification notification = Notification.builder()
                .meeting(meeting)
                .recipient(recipient)
                .type(type)
                .message(message)
                .sentAt(LocalDateTime.now())
                .status(NotificationStatus.SENT)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification [{}] sent to user {}: {}", type, recipient != null ? recipient.getEmail() : "N/A", message);

        // Multi-Channel Dispatch: Forward to Telegram if recipient has linked Chat ID
        if (recipient != null && Boolean.TRUE.equals(recipient.getTelegramNotificationsEnabled())
                && recipient.getTelegramChatId() != null && !recipient.getTelegramChatId().isBlank()) {
            Integer leadMinutes = recipient.getTelegramReminderMinutes() != null ? recipient.getTelegramReminderMinutes() : 10;
            telegramService.sendDirectNotification(recipient.getTelegramChatId(), type, meeting, leadMinutes, message);
        }

        return mapToResponse(saved);
    }

    public void broadcastMeetingNotification(Meeting meeting, NotificationType type, String message) {
        if (meeting.getOrganizer() != null) {
            createNotification(meeting, meeting.getOrganizer(), type, message);
        }

        if (meeting.getAttendees() != null) {
            meeting.getAttendees().forEach(attendee -> {
                if (attendee.getUser() != null &&
                        (meeting.getOrganizer() == null || !attendee.getUser().getUserId().equals(meeting.getOrganizer().getUserId()))) {
                    createNotification(meeting, attendee.getUser(), type, message);
                }
            });
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(Long userId) {
        return notificationRepository.findByRecipient_UserIdOrderBySentAtDesc(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public void markNotificationStatus(Long notificationId, NotificationStatus status) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));
        notification.setStatus(status);
        notificationRepository.save(notification);
    }

    public void markAllAsRead(Long userId) {
        List<Notification> unreadList = notificationRepository.findByRecipient_UserIdAndStatusNot(userId, NotificationStatus.READ);
        unreadList.forEach(n -> n.setStatus(NotificationStatus.READ));
        notificationRepository.saveAll(unreadList);
    }

    public NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .meetingId(notification.getMeeting() != null ? notification.getMeeting().getMeetingId() : null)
                .meetingTitle(notification.getMeeting() != null ? notification.getMeeting().getTitle() : null)
                .recipientId(notification.getRecipient() != null ? notification.getRecipient().getUserId() : null)
                .recipientName(notification.getRecipient() != null ? notification.getRecipient().getName() : null)
                .type(notification.getType())
                .message(notification.getMessage())
                .sentAt(notification.getSentAt())
                .status(notification.getStatus())
                .build();
    }
}
