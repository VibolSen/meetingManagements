package Vibol.SEN.meetingManagements.service;

import Vibol.SEN.meetingManagements.dto.MeetingMinutesDTO;
import Vibol.SEN.meetingManagements.dto.MeetingMinutesRequest;
import Vibol.SEN.meetingManagements.exception.ResourceNotFoundException;
import Vibol.SEN.meetingManagements.model.Meeting;
import Vibol.SEN.meetingManagements.model.MeetingMinutes;
import Vibol.SEN.meetingManagements.model.User;
import Vibol.SEN.meetingManagements.model.enums.AuditActionType;
import Vibol.SEN.meetingManagements.model.enums.AuditEntityType;
import Vibol.SEN.meetingManagements.model.enums.NotificationType;
import Vibol.SEN.meetingManagements.repository.MeetingMinutesRepository;
import Vibol.SEN.meetingManagements.repository.MeetingRepository;
import Vibol.SEN.meetingManagements.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MeetingMinutesService {

    private final MeetingMinutesRepository meetingMinutesRepository;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public MeetingMinutesDTO getMinutes(Long meetingId) {
        MeetingMinutes minutes = meetingMinutesRepository.findByMeeting_MeetingId(meetingId)
                .orElse(null);
        return minutes != null ? mapToDTO(minutes) : null;
    }

    public MeetingMinutesDTO saveOrUpdateMinutes(Long meetingId, MeetingMinutesRequest request) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with ID: " + meetingId));

        User author = null;
        if (request.getPublishedById() != null) {
            author = userRepository.findById(request.getPublishedById()).orElse(meeting.getOrganizer());
        } else {
            author = meeting.getOrganizer();
        }

        MeetingMinutes minutes = meetingMinutesRepository.findByMeeting_MeetingId(meetingId)
                .orElse(MeetingMinutes.builder()
                        .meeting(meeting)
                        .build());

        minutes.setAgenda(request.getAgenda());
        minutes.setSummary(request.getSummary());
        minutes.setKeyDecisions(request.getKeyDecisions());
        minutes.setPublishedBy(author);
        minutes.setPublishedAt(LocalDateTime.now());

        MeetingMinutes saved = meetingMinutesRepository.save(minutes);

        // Notify attendees that Meeting Minutes are ready
        notificationService.broadcastMeetingNotification(
                meeting,
                NotificationType.CHANGE,
                "Official Meeting Minutes (MOM) for '" + meeting.getTitle() + "' have been published."
        );

        if (author != null) {
            auditLogService.recordLog(
                    author.getUserId(),
                    author.getName(),
                    author.getEmail(),
                    AuditActionType.UPDATE,
                    AuditEntityType.MEETING,
                    meeting.getMeetingId(),
                    meeting.getTitle(),
                    "Published meeting minutes and executive summary",
                    "127.0.0.1"
            );
        }

        return mapToDTO(saved);
    }

    private MeetingMinutesDTO mapToDTO(MeetingMinutes m) {
        return MeetingMinutesDTO.builder()
                .minutesId(m.getMinutesId())
                .meetingId(m.getMeeting().getMeetingId())
                .agenda(m.getAgenda())
                .summary(m.getSummary())
                .keyDecisions(m.getKeyDecisions())
                .publishedAt(m.getPublishedAt())
                .publishedBy(m.getPublishedBy() != null ? userService.mapToDTO(m.getPublishedBy()) : null)
                .build();
    }
}
