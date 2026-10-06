package Vibol.SEN.meetingManagements.service;

import Vibol.SEN.meetingManagements.model.Meeting;
import Vibol.SEN.meetingManagements.model.MeetingAttendee;
import Vibol.SEN.meetingManagements.model.Notification;
import Vibol.SEN.meetingManagements.model.User;
import Vibol.SEN.meetingManagements.model.enums.MeetingStatus;
import Vibol.SEN.meetingManagements.model.enums.NotificationStatus;
import Vibol.SEN.meetingManagements.model.enums.NotificationType;
import Vibol.SEN.meetingManagements.repository.MeetingRepository;
import Vibol.SEN.meetingManagements.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingReminderScheduler {

    private final MeetingRepository meetingRepository;
    private final NotificationRepository notificationRepository;
    private final TelegramService telegramService;

    /**
     * Executes every 60 seconds to inspect upcoming confirmed meetings
     * and dispatch dynamic advance countdown reminders tailored to each participant's settings.
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void processUpcomingMeetingReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime horizon = now.plusMinutes(120);

        List<Meeting> upcomingMeetings = meetingRepository.findByStatusAndStartTimeBetween(
                MeetingStatus.CONFIRMED, now, horizon
        );

        if (upcomingMeetings.isEmpty()) {
            return;
        }

        for (Meeting meeting : upcomingMeetings) {
            // 1. Process Organizer Reminder
            if (meeting.getOrganizer() != null) {
                processUserReminder(meeting, meeting.getOrganizer(), now);
            }

            // 2. Process Attendees Reminders
            if (meeting.getAttendees() != null) {
                for (MeetingAttendee attendee : meeting.getAttendees()) {
                    if (attendee.getUser() != null) {
                        processUserReminder(meeting, attendee.getUser(), now);
                    }
                }
            }
        }
    }

    private void processUserReminder(Meeting meeting, User user, LocalDateTime now) {
        if (user == null || user.getUserId() == null) {
            return;
        }

        // Check if user has Telegram alerts enabled and a valid Chat ID
        if (!Boolean.TRUE.equals(user.getTelegramNotificationsEnabled()) ||
                user.getTelegramChatId() == null || user.getTelegramChatId().isBlank()) {
            return;
        }

        // Dynamic per-user lead time (default 10 minutes)
        int leadMinutes = user.getTelegramReminderMinutes() != null ? user.getTelegramReminderMinutes() : 10;
        LocalDateTime reminderWindowStart = meeting.getStartTime().minusMinutes(leadMinutes + 1);
        LocalDateTime meetingStartTime = meeting.getStartTime();

        // Check if current time falls within user's reminder window
        if (now.isAfter(reminderWindowStart) && now.isBefore(meetingStartTime)) {
            // Prevent duplicate reminders for this meeting and user
            boolean alreadyReminded = notificationRepository.existsByMeeting_MeetingIdAndRecipient_UserIdAndType(
                    meeting.getMeetingId(), user.getUserId(), NotificationType.REMINDER
            );

            if (!alreadyReminded) {
                log.info("Dispatching {}m pre-meeting reminder for meeting [{}] to user [{}] (Chat ID: {})",
                        leadMinutes, meeting.getTitle(), user.getEmail(), user.getTelegramChatId());

                // 1. Dispatch custom Telegram Markdown alert
                telegramService.sendDirectNotification(
                        user.getTelegramChatId(),
                        NotificationType.REMINDER,
                        meeting,
                        leadMinutes,
                        null
                );

                // 2. Record in database to ensure deduplication and in-app alert visibility
                Notification reminderNotification = Notification.builder()
                        .meeting(meeting)
                        .recipient(user)
                        .type(NotificationType.REMINDER)
                        .message("Reminder: \"" + meeting.getTitle() + "\" starts in " + leadMinutes + " minutes at " +
                                (meeting.getRoom() != null ? meeting.getRoom().getName() : "designated room"))
                        .sentAt(now)
                        .status(NotificationStatus.SENT)
                        .build();

                notificationRepository.save(reminderNotification);
            }
        }
    }
}
