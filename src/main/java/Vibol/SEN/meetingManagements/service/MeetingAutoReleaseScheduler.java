package Vibol.SEN.meetingManagements.service;

import Vibol.SEN.meetingManagements.model.Meeting;
import Vibol.SEN.meetingManagements.model.enums.MeetingStatus;
import Vibol.SEN.meetingManagements.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingAutoReleaseScheduler {

    private final MeetingRepository meetingRepository;
    private final MeetingService meetingService;
    private final SystemSettingService systemSettingService;

    /**
     * Checks every 60 seconds for confirmed meetings where no check-in occurred within the grace period.
     * Automatically cancels the meeting, releases room & equipment, and notifies organizer.
     */
    @Scheduled(fixedDelay = 60000)
    public void autoReleaseUncheckedMeetings() {
        int graceMinutes = systemSettingService.getInt("booking.no_show_auto_release_minutes", 15);
        if (graceMinutes <= 0) {
            return; // Auto-release feature disabled if set to 0 or negative
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.minusMinutes(graceMinutes);

        List<Meeting> noShowMeetings = meetingRepository.findUncheckedMeetingsPastDeadline(
                MeetingStatus.CONFIRMED,
                deadline,
                now
        );

        if (!noShowMeetings.isEmpty()) {
            log.info("Found {} meetings past check-in grace period ({} min). Auto-releasing...", noShowMeetings.size(), graceMinutes);
            for (Meeting meeting : noShowMeetings) {
                try {
                    String reason = "AUTO_RELEASE_NO_SHOW: Room released automatically due to absence of check-in within " + graceMinutes + " minutes";
                    meetingService.cancelMeeting(meeting.getMeetingId(), reason);
                    log.info("Auto-released meeting ID #{} ('{}') in room '{}'",
                            meeting.getMeetingId(), meeting.getTitle(), meeting.getRoom().getName());
                } catch (Exception ex) {
                    log.error("Failed to auto-release meeting ID #{}: {}", meeting.getMeetingId(), ex.getMessage(), ex);
                }
            }
        }
    }
}
