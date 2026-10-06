package Vibol.SEN.meetingManagements.service;

import Vibol.SEN.meetingManagements.dto.*;
import Vibol.SEN.meetingManagements.exception.BadRequestException;
import Vibol.SEN.meetingManagements.exception.ConflictException;
import Vibol.SEN.meetingManagements.exception.ResourceNotFoundException;
import Vibol.SEN.meetingManagements.model.*;
import Vibol.SEN.meetingManagements.model.enums.*;
import Vibol.SEN.meetingManagements.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RecurringMeetingService {

    private final RecurringSeriesRepository recurringSeriesRepository;
    private final MeetingRepository meetingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final MeetingAttendeeRepository attendeeRepository;
    private final MeetingMaterialRepository meetingMaterialRepository;
    private final MeetingStaffRepository meetingStaffRepository;
    private final MaterialRepository materialRepository;
    private final StaffRepository staffRepository;
    private final NotificationService notificationService;
    private final SystemSettingService systemSettingService;
    private final AuditLogService auditLogService;
    private final MeetingService meetingService;

    @Transactional(readOnly = true)
    public RecurringPreviewResponse previewRecurrence(RecurringMeetingRequest request) {
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + request.getRoomId()));

        List<LocalDateTime[]> instances = generateOccurrenceWindows(request);
        List<RecurringPreviewResponse.RecurringSlotDTO> slotDTOs = new ArrayList<>();
        int clearCount = 0;
        int conflictCount = 0;

        for (LocalDateTime[] slot : instances) {
            LocalDateTime start = slot[0];
            LocalDateTime end = slot[1];

            List<Meeting> conflicts = meetingRepository.findOverlappingMeetings(room.getRoomId(), start, end);
            boolean isAvailable = conflicts.isEmpty();
            String conflictReason = isAvailable ? null : "Occupied by: '" + conflicts.get(0).getTitle() + "'";

            if (isAvailable) {
                clearCount++;
            } else {
                conflictCount++;
            }

            slotDTOs.add(RecurringPreviewResponse.RecurringSlotDTO.builder()
                    .startTime(start)
                    .endTime(end)
                    .isAvailable(isAvailable)
                    .conflictReason(conflictReason)
                    .build());
        }

        return RecurringPreviewResponse.builder()
                .totalGenerated(instances.size())
                .clearCount(clearCount)
                .conflictCount(conflictCount)
                .slots(slotDTOs)
                .build();
    }

    public List<MeetingResponse> createRecurringSeries(RecurringMeetingRequest request) {
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + request.getRoomId()));

        User organizer = userRepository.findById(request.getOrganizerId())
                .orElseThrow(() -> new ResourceNotFoundException("Organizer not found with ID: " + request.getOrganizerId()));

        List<LocalDateTime[]> occurrences = generateOccurrenceWindows(request);
        if (occurrences.isEmpty()) {
            throw new BadRequestException("No recurring occurrences could be scheduled based on the provided pattern.");
        }

        // Save Series
        RecurringSeries series = RecurringSeries.builder()
                .recurrenceType(request.getRecurrenceType())
                .repeatInterval(request.getRepeatInterval() != null ? request.getRepeatInterval() : 1)
                .daysOfWeek(request.getDaysOfWeek())
                .endDate(request.getEndDate())
                .totalOccurrences(occurrences.size())
                .build();
        RecurringSeries savedSeries = recurringSeriesRepository.save(series);

        List<Meeting> createdMeetings = new ArrayList<>();
        int threshold = systemSettingService.getInt("booking.approval_threshold_capacity", 20);
        boolean requiresApproval = (room.getCapacity() >= threshold);
        MeetingStatus initialStatus = requiresApproval ? MeetingStatus.PENDING : MeetingStatus.CONFIRMED;

        String attendeeMode = systemSettingService.getString("booking.attendee_acceptance_mode", "AUTO_ACCEPT");
        AttendeeResponseStatus initialAttendeeStatus = "AUTO_ACCEPT".equalsIgnoreCase(attendeeMode)
                ? AttendeeResponseStatus.ACCEPTED
                : AttendeeResponseStatus.PENDING;

        for (LocalDateTime[] slot : occurrences) {
            LocalDateTime start = slot[0];
            LocalDateTime end = slot[1];

            List<Meeting> conflicts = meetingRepository.findOverlappingMeetings(room.getRoomId(), start, end);
            if (!conflicts.isEmpty()) {
                if (Boolean.TRUE.equals(request.getSkipConflictedDates())) {
                    log.info("Skipping conflicting recurring slot: {} - {}", start, end);
                    continue;
                } else {
                    throw new ConflictException("Conflicting meeting detected on " + start.toLocalDate() + ": '" + conflicts.get(0).getTitle() + "'");
                }
            }

            Meeting meeting = Meeting.builder()
                    .title(request.getTitle())
                    .purpose(request.getPurpose())
                    .organizer(organizer)
                    .room(room)
                    .startTime(start)
                    .endTime(end)
                    .status(initialStatus)
                    .recurringSeries(savedSeries)
                    .build();

            Meeting savedMeeting = meetingRepository.save(meeting);

            // Attendees
            if (request.getAttendeeIds() != null) {
                for (Long userId : request.getAttendeeIds()) {
                    userRepository.findById(userId).ifPresent(u -> {
                        MeetingAttendee attendee = MeetingAttendee.builder()
                                .id(new MeetingAttendeeId(savedMeeting.getMeetingId(), u.getUserId()))
                                .meeting(savedMeeting)
                                .user(u)
                                .responseStatus(initialAttendeeStatus)
                                .build();
                        attendeeRepository.save(attendee);
                    });
                }
            }

            // Materials
            if (request.getMaterials() != null) {
                for (MeetingMaterialRequest matReq : request.getMaterials()) {
                    materialRepository.findById(matReq.getMaterialId()).ifPresent(mat -> {
                        MeetingMaterial mm = MeetingMaterial.builder()
                                .id(new MeetingMaterialId(savedMeeting.getMeetingId(), mat.getMaterialId()))
                                .meeting(savedMeeting)
                                .material(mat)
                                .quantityRequested(matReq.getQuantityRequested())
                                .build();
                        meetingMaterialRepository.save(mm);
                    });
                }
            }

            // Staff
            if (request.getStaffAssignments() != null) {
                for (MeetingStaffRequest staffReq : request.getStaffAssignments()) {
                    staffRepository.findById(staffReq.getStaffId()).ifPresent(st -> {
                        MeetingStaff ms = MeetingStaff.builder()
                                .id(new MeetingStaffId(savedMeeting.getMeetingId(), st.getStaffId()))
                                .meeting(savedMeeting)
                                .staff(st)
                                .assignedRole(staffReq.getAssignedRole())
                                .build();
                        meetingStaffRepository.save(ms);
                    });
                }
            }

            // Notification
            notificationService.broadcastMeetingNotification(
                    savedMeeting,
                    NotificationType.CONFIRMATION,
                    "Your recurring session for '" + savedMeeting.getTitle() + "' has been scheduled."
            );
            createdMeetings.add(savedMeeting);
        }

        auditLogService.recordLog(
                organizer.getUserId(),
                organizer.getName(),
                organizer.getEmail(),
                AuditActionType.CREATE,
                AuditEntityType.MEETING,
                savedSeries.getSeriesId(),
                "Recurring Series #" + savedSeries.getSeriesId(),
                "Created recurring meeting series with " + createdMeetings.size() + " sessions confirmed",
                "127.0.0.1"
        );

        return createdMeetings.stream()
                .map(meetingService::mapToResponse)
                .collect(Collectors.toList());
    }

    private List<LocalDateTime[]> generateOccurrenceWindows(RecurringMeetingRequest request) {
        List<LocalDateTime[]> windows = new ArrayList<>();
        LocalDateTime baseStart = request.getStartTime();
        LocalDateTime baseEnd = request.getEndTime();
        Duration duration = Duration.between(baseStart, baseEnd);

        int interval = request.getRepeatInterval() != null && request.getRepeatInterval() > 0 ? request.getRepeatInterval() : 1;
        int maxOccurrences = request.getOccurrencesCount() != null && request.getOccurrencesCount() > 0
                ? request.getOccurrencesCount()
                : 30; // safety ceiling

        int maxAdvanceDays = systemSettingService.getInt("booking.max_advance_days", 90);
        LocalDate limitDate = LocalDate.now().plusDays(maxAdvanceDays);
        if (request.getEndDate() != null && request.getEndDate().isBefore(limitDate)) {
            limitDate = request.getEndDate();
        }

        boolean allowWeekend = systemSettingService.getBoolean("booking.allow_weekend_booking", false);

        Set<DayOfWeek> targetDays = parseDaysOfWeek(request.getDaysOfWeek());
        if (targetDays.isEmpty()) {
            targetDays.add(baseStart.getDayOfWeek());
        }

        LocalDateTime currentStart = baseStart;
        while (windows.size() < maxOccurrences && !currentStart.toLocalDate().isAfter(limitDate)) {
            DayOfWeek dow = currentStart.getDayOfWeek();
            boolean isWeekend = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);

            if ((allowWeekend || !isWeekend) && targetDays.contains(dow)) {
                windows.add(new LocalDateTime[]{currentStart, currentStart.plus(duration)});
            }

            switch (request.getRecurrenceType()) {
                case DAILY:
                    currentStart = currentStart.plusDays(interval);
                    break;
                case WEEKLY:
                    // If multiple days selected (e.g. Mon & Wed), step by 1 day until match
                    if (targetDays.size() > 1) {
                        currentStart = currentStart.plusDays(1);
                    } else {
                        currentStart = currentStart.plusWeeks(interval);
                    }
                    break;
                case BI_WEEKLY:
                    currentStart = currentStart.plusWeeks(2L * interval);
                    break;
                case MONTHLY:
                    currentStart = currentStart.plusMonths(interval);
                    break;
                default:
                    // NONE
                    return windows;
            }
        }

        return windows;
    }

    private Set<DayOfWeek> parseDaysOfWeek(String days) {
        Set<DayOfWeek> set = new HashSet<>();
        if (days == null || days.isBlank()) return set;
        for (String part : days.split(",")) {
            try {
                set.add(DayOfWeek.valueOf(part.trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        return set;
    }
}
