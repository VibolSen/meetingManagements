package Vibol.SEN.meetingManagements.service;

import Vibol.SEN.meetingManagements.dto.MeetingResponse;
import Vibol.SEN.meetingManagements.dto.RoomDisplayStatusDTO;
import Vibol.SEN.meetingManagements.dto.RoomIssueDTO;
import Vibol.SEN.meetingManagements.exception.ResourceNotFoundException;
import Vibol.SEN.meetingManagements.model.Meeting;
import Vibol.SEN.meetingManagements.model.Room;
import Vibol.SEN.meetingManagements.model.enums.MeetingStatus;
import Vibol.SEN.meetingManagements.model.enums.RoomIssueStatus;
import Vibol.SEN.meetingManagements.repository.MeetingRepository;
import Vibol.SEN.meetingManagements.repository.RoomIssueRepository;
import Vibol.SEN.meetingManagements.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomDisplayService {

    private final RoomRepository roomRepository;
    private final RoomService roomService;
    private final MeetingRepository meetingRepository;
    private final MeetingService meetingService;
    private final RoomIssueRepository roomIssueRepository;
    private final RoomIssueService roomIssueService;

    @Transactional(readOnly = true)
    public RoomDisplayStatusDTO getRoomDisplayStatus(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        List<Meeting> todayMeetings = meetingRepository.findTodayScheduleForRoom(roomId, startOfDay, endOfDay);

        // Find current active confirmed meeting
        Meeting currentMeetingEntity = todayMeetings.stream()
                .filter(m -> m.getStatus() == MeetingStatus.CONFIRMED
                        && !m.getStartTime().isAfter(now)
                        && m.getEndTime().isAfter(now))
                .findFirst()
                .orElse(null);

        // Find next upcoming confirmed meeting today
        Meeting nextMeetingEntity = todayMeetings.stream()
                .filter(m -> m.getStatus() == MeetingStatus.CONFIRMED
                        && m.getStartTime().isAfter(now))
                .findFirst()
                .orElse(null);

        Long secondsRemaining = null;
        if (currentMeetingEntity != null) {
            secondsRemaining = Math.max(0, Duration.between(now, currentMeetingEntity.getEndTime()).getSeconds());
        }

        Long minutesUntilNext = null;
        if (nextMeetingEntity != null) {
            minutesUntilNext = Math.max(0, Duration.between(now, nextMeetingEntity.getStartTime()).toMinutes());
        }

        List<MeetingResponse> todaySchedule = todayMeetings.stream()
                .map(meetingService::mapToResponse)
                .collect(Collectors.toList());

        List<RoomIssueDTO> activeIssues = roomIssueRepository.findByRoom_RoomIdOrderByCreatedAtDesc(roomId).stream()
                .filter(i -> i.getStatus() == RoomIssueStatus.OPEN || i.getStatus() == RoomIssueStatus.IN_PROGRESS)
                .map(roomIssueService::mapToDTO)
                .collect(Collectors.toList());

        return RoomDisplayStatusDTO.builder()
                .room(roomService.mapToResponse(room))
                .isOccupied(currentMeetingEntity != null)
                .currentMeeting(currentMeetingEntity != null ? meetingService.mapToResponse(currentMeetingEntity) : null)
                .nextMeeting(nextMeetingEntity != null ? meetingService.mapToResponse(nextMeetingEntity) : null)
                .secondsRemainingInCurrentMeeting(secondsRemaining)
                .minutesUntilNextMeeting(minutesUntilNext)
                .currentTime(now)
                .todaySchedule(todaySchedule)
                .activeIssues(activeIssues)
                .build();
    }
}
