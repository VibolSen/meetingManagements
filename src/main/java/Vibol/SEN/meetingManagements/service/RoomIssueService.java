package Vibol.SEN.meetingManagements.service;

import Vibol.SEN.meetingManagements.dto.RoomIssueCreateRequest;
import Vibol.SEN.meetingManagements.dto.RoomIssueDTO;
import Vibol.SEN.meetingManagements.dto.RoomIssueUpdateRequest;
import Vibol.SEN.meetingManagements.exception.ResourceNotFoundException;
import Vibol.SEN.meetingManagements.model.Room;
import Vibol.SEN.meetingManagements.model.RoomIssue;
import Vibol.SEN.meetingManagements.model.User;
import Vibol.SEN.meetingManagements.model.enums.AuditActionType;
import Vibol.SEN.meetingManagements.model.enums.AuditEntityType;
import Vibol.SEN.meetingManagements.model.enums.IssuePriority;
import Vibol.SEN.meetingManagements.model.enums.RoomIssueStatus;
import Vibol.SEN.meetingManagements.model.enums.RoomStatus;
import Vibol.SEN.meetingManagements.repository.RoomIssueRepository;
import Vibol.SEN.meetingManagements.repository.RoomRepository;
import Vibol.SEN.meetingManagements.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomIssueService {

    private final RoomIssueRepository roomIssueRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final TelegramService telegramService;

    @Transactional
    public RoomIssueDTO reportIssue(Long roomId, RoomIssueCreateRequest request, Long userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));

        User reporter = null;
        if (userId != null) {
            reporter = userRepository.findById(userId).orElse(null);
        } else if (request.getReportedById() != null) {
            reporter = userRepository.findById(request.getReportedById()).orElse(null);
        }

        String reporterDisplayName = request.getReporterName();
        if (reporterDisplayName == null || reporterDisplayName.isBlank()) {
            reporterDisplayName = reporter != null ? reporter.getName() : "Room Kiosk";
        }

        RoomIssue issue = RoomIssue.builder()
                .room(room)
                .reportedBy(reporter)
                .reporterName(reporterDisplayName)
                .category(request.getCategory())
                .priority(request.getPriority())
                .title(request.getTitle())
                .description(request.getDescription())
                .status(RoomIssueStatus.OPEN)
                .build();

        // If High or Critical priority, flag room as UNDER_MAINTENANCE immediately
        if (request.getPriority() == IssuePriority.HIGH || request.getPriority() == IssuePriority.CRITICAL) {
            room.setStatus(RoomStatus.UNDER_MAINTENANCE);
            roomRepository.save(room);
            log.warn("Room #{} ('{}') automatically flagged UNDER_MAINTENANCE due to {} priority issue '{}'",
                    room.getRoomId(), room.getName(), request.getPriority(), request.getTitle());
        }

        RoomIssue saved = roomIssueRepository.save(issue);

        // Audit Logging
        auditLogService.recordLog(
                reporter != null ? reporter.getUserId() : null,
                reporterDisplayName,
                reporter != null ? reporter.getEmail() : "kiosk@system.local",
                AuditActionType.CREATE,
                AuditEntityType.ROOM,
                room.getRoomId(),
                room.getName(),
                "Reported facility issue [" + request.getPriority() + "] " + request.getTitle(),
                "127.0.0.1"
        );

        // Dispatch Telegram Notification to Facilities Channel
        try {
            String alertMessage = "⚠️ *Facility Maintenance Alert*\n" +
                    "📍 *Room:* " + room.getName() + " (" + room.getLocation() + ")\n" +
                    "🚨 *Priority:* " + request.getPriority() + "\n" +
                    "🏷️ *Category:* " + request.getCategory() + "\n" +
                    "📌 *Issue:* " + request.getTitle() + "\n" +
                    (request.getDescription() != null ? "📝 *Details:* " + request.getDescription() + "\n" : "") +
                    "👤 *Reported by:* " + reporterDisplayName;
            telegramService.sendMessage(telegramService.getDefaultChatId(), alertMessage);
        } catch (Exception ex) {
            log.error("Failed to dispatch facility issue telegram alert: {}", ex.getMessage());
        }

        return mapToDTO(saved);
    }

    public List<RoomIssueDTO> getIssuesForRoom(Long roomId) {
        return roomIssueRepository.findByRoom_RoomIdOrderByCreatedAtDesc(roomId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<RoomIssueDTO> getAllIssues() {
        return roomIssueRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public RoomIssueDTO updateIssueStatus(Long issueId, RoomIssueUpdateRequest request) {
        RoomIssue issue = roomIssueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Room issue not found with ID: " + issueId));

        issue.setStatus(request.getStatus());
        if (request.getResolutionNotes() != null) {
            issue.setResolutionNotes(request.getResolutionNotes());
        }

        if (request.getStatus() == RoomIssueStatus.RESOLVED || request.getStatus() == RoomIssueStatus.CLOSED) {
            issue.setResolvedAt(LocalDateTime.now());

            long openIssuesCount = roomIssueRepository.countByRoom_RoomIdAndStatusIn(
                    issue.getRoom().getRoomId(),
                    List.of(RoomIssueStatus.OPEN, RoomIssueStatus.IN_PROGRESS)
            );
            if (openIssuesCount <= 1 && issue.getRoom().getStatus() == RoomStatus.UNDER_MAINTENANCE) {
                issue.getRoom().setStatus(RoomStatus.ACTIVE);
                roomRepository.save(issue.getRoom());
                log.info("Room #{} ('{}') restored to ACTIVE status following issue resolution",
                        issue.getRoom().getRoomId(), issue.getRoom().getName());
            }
        }

        RoomIssue updated = roomIssueRepository.save(issue);
        return mapToDTO(updated);
    }

    public RoomIssueDTO mapToDTO(RoomIssue issue) {
        return RoomIssueDTO.builder()
                .issueId(issue.getIssueId())
                .roomId(issue.getRoom().getRoomId())
                .roomName(issue.getRoom().getName())
                .reportedById(issue.getReportedBy() != null ? issue.getReportedBy().getUserId() : null)
                .reportedByName(issue.getReportedBy() != null ? issue.getReportedBy().getName() : null)
                .reporterName(issue.getReporterName())
                .category(issue.getCategory())
                .priority(issue.getPriority())
                .title(issue.getTitle())
                .description(issue.getDescription())
                .status(issue.getStatus())
                .resolutionNotes(issue.getResolutionNotes())
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt())
                .resolvedAt(issue.getResolvedAt())
                .build();
    }
}
