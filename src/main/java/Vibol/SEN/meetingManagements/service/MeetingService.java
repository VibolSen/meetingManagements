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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final MaterialRepository materialRepository;
    private final StaffRepository staffRepository;
    private final MeetingAttendeeRepository attendeeRepository;
    private final MeetingMaterialRepository meetingMaterialRepository;
    private final MeetingStaffRepository meetingStaffRepository;
    private final MaterialService materialService;
    private final NotificationService notificationService;
    private final UserService userService;
    private final RoomService roomService;
    private final AuditLogService auditLogService;
    private final SystemSettingService systemSettingService;
    private final EmailNotificationService emailNotificationService;

    @Transactional(readOnly = true)
    public List<MeetingResponse> getAllMeetings() {
        return meetingRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MeetingResponse getMeetingById(Long id) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with ID: " + id));
        return mapToResponse(meeting);
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> getMeetingsByOrganizer(Long organizerId) {
        return meetingRepository.findByOrganizer_UserId(organizerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> getMeetingsByStatus(MeetingStatus status) {
        return meetingRepository.findByStatus(status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> getMeetingsForCalendar(Long roomId, LocalDateTime start, LocalDateTime end) {
        if (roomId != null) {
            return meetingRepository.findByRoom_RoomIdAndStartTimeBetween(roomId, start, end).stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }
        return meetingRepository.findByStartTimeBetween(start, end).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public MeetingResponse createMeeting(MeetingCreateRequest request) {
        validateMeetingTimes(request.getStartTime(), request.getEndTime());

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + request.getRoomId()));

        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new BadRequestException("Selected room is not active. Status: " + room.getStatus());
        }

        // 1. Conflict Prevention: Double-booking validation
        if (meetingRepository.existsOverlappingMeeting(room.getRoomId(), request.getStartTime(), request.getEndTime())) {
            List<Meeting> conflicts = meetingRepository.findOverlappingMeetings(room.getRoomId(), request.getStartTime(), request.getEndTime());
            String conflictTitle = conflicts.isEmpty() ? "Unknown" : conflicts.get(0).getTitle();
            throw new ConflictException("Room '" + room.getName() + "' is already booked during this time interval by meeting: '" + conflictTitle + "'");
        }

        User organizer = userRepository.findById(request.getOrganizerId())
                .orElseThrow(() -> new ResourceNotFoundException("Organizer not found with ID: " + request.getOrganizerId()));

        // Individual Staff Access Control: Check if user is restricted to View Only (e.g. probation staff, intern)
        if (organizer.getBookingAccess() == BookingAccessLevel.VIEW_ONLY) {
            throw new BadRequestException("Your account is currently set to 'View Only' access (e.g. probationary period) and is restricted from booking meeting rooms. Please contact your administrator.");
        }

        // Dynamic Role & Policy Validations
        if (organizer.getRole() == UserRole.EMPLOYEE && !systemSettingService.getBoolean("role.employee.can_book", true)) {
            throw new BadRequestException("Meeting reservation by employees is currently disabled by administrative policy.");
        }

        int maxDays = systemSettingService.getInt("booking.max_advance_days", 60);
        if (request.getStartTime().isAfter(LocalDateTime.now().plusDays(maxDays))) {
            throw new BadRequestException("Reservations cannot be made more than " + maxDays + " days in advance.");
        }

        boolean allowWeekend = systemSettingService.getBoolean("booking.allow_weekend_booking", false);
        DayOfWeek day = request.getStartTime().getDayOfWeek();
        if (!allowWeekend && (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY)) {
            throw new BadRequestException("Weekend reservations are not allowed under current facility policy.");
        }

        String startHourStr = systemSettingService.getString("booking.operating_hours_start", "08:00");
        String endHourStr = systemSettingService.getString("booking.operating_hours_end", "18:00");
        try {
            LocalTime openingTime = LocalTime.parse(startHourStr);
            LocalTime closingTime = LocalTime.parse(endHourStr);
            LocalTime meetingStart = request.getStartTime().toLocalTime();
            LocalTime meetingEnd = request.getEndTime().toLocalTime();
            if (meetingStart.isBefore(openingTime) || meetingEnd.isAfter(closingTime)) {
                throw new BadRequestException("Meeting must fall within facility operating hours (" + startHourStr + " - " + endHourStr + ").");
            }
        } catch (DateTimeParseException ignored) {}

        if (organizer.getRole() == UserRole.EMPLOYEE && request.getMaterials() != null && !request.getMaterials().isEmpty()) {
            boolean canRequestMaterials = systemSettingService.getBoolean("role.employee.can_request_materials", true);
            if (!canRequestMaterials) {
                throw new BadRequestException("Material requests by employees are disabled by policy.");
            }
        }

        // 2. Dynamic Approval workflow: Threshold-based or role-based requirement
        int threshold = systemSettingService.getInt("booking.approval_threshold_capacity", 20);
        boolean employeeRequireApproval = systemSettingService.getBoolean("role.employee.require_approval", false);
        boolean organizerRequireApproval = systemSettingService.getBoolean("role.organizer.require_approval", false);

        boolean requiresApproval = (room.getCapacity() >= threshold);
        if (organizer.getRole() == UserRole.EMPLOYEE && employeeRequireApproval) {
            requiresApproval = true;
        } else if (organizer.getRole() == UserRole.ORGANIZER && organizerRequireApproval) {
            requiresApproval = true;
        }

        MeetingStatus initialStatus = requiresApproval ? MeetingStatus.PENDING : MeetingStatus.CONFIRMED;

        Meeting meeting = Meeting.builder()
                .title(request.getTitle())
                .purpose(request.getPurpose())
                .organizer(organizer)
                .room(room)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(initialStatus)
                .build();

        Meeting savedMeeting = meetingRepository.save(meeting);

        // 3. Attach attendees with dynamic acceptance mode
        String attendeeMode = systemSettingService.getString("booking.attendee_acceptance_mode", "AUTO_ACCEPT");
        AttendeeResponseStatus initialAttendeeStatus = "AUTO_ACCEPT".equalsIgnoreCase(attendeeMode)
                ? AttendeeResponseStatus.ACCEPTED
                : AttendeeResponseStatus.PENDING;

        if (request.getAttendeeIds() != null) {
            for (Long userId : request.getAttendeeIds()) {
                User attendeeUser = userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Attendee user not found with ID: " + userId));

                MeetingAttendee attendee = MeetingAttendee.builder()
                        .id(new MeetingAttendeeId(savedMeeting.getMeetingId(), attendeeUser.getUserId()))
                        .meeting(savedMeeting)
                        .user(attendeeUser)
                        .responseStatus(initialAttendeeStatus)
                        .build();
                MeetingAttendee savedAttendee = attendeeRepository.save(attendee);
                savedMeeting.getAttendees().add(savedAttendee);
            }
        }

        // 4. Reserve materials & equipment
        if (request.getMaterials() != null) {
            for (MeetingMaterialRequest matReq : request.getMaterials()) {
                materialService.reserveMaterialStock(matReq.getMaterialId(), matReq.getQuantityRequested());
                Material material = materialRepository.findById(matReq.getMaterialId()).orElseThrow();

                MeetingMaterial mm = MeetingMaterial.builder()
                        .id(new MeetingMaterialId(savedMeeting.getMeetingId(), material.getMaterialId()))
                        .meeting(savedMeeting)
                        .material(material)
                        .quantityRequested(matReq.getQuantityRequested())
                        .build();
                MeetingMaterial savedMm = meetingMaterialRepository.save(mm);
                savedMeeting.getMaterials().add(savedMm);
            }
        }

        // 5. Staff assignment & validation
        if (request.getStaffAssignments() != null) {
            for (MeetingStaffRequest staffReq : request.getStaffAssignments()) {
                Staff staff = staffRepository.findById(staffReq.getStaffId())
                        .orElseThrow(() -> new ResourceNotFoundException("Staff not found with ID: " + staffReq.getStaffId()));

                MeetingStaff ms = MeetingStaff.builder()
                        .id(new MeetingStaffId(savedMeeting.getMeetingId(), staff.getStaffId()))
                        .meeting(savedMeeting)
                        .staff(staff)
                        .assignedRole(staffReq.getAssignedRole() != null ? staffReq.getAssignedRole() : staff.getRole().name())
                        .build();
                MeetingStaff savedMs = meetingStaffRepository.save(ms);
                savedMeeting.getStaffAssignments().add(savedMs);
            }
        }

        // 6. Broadcast notification
        String msg = initialStatus == MeetingStatus.CONFIRMED
                ? "Meeting confirmed: '" + savedMeeting.getTitle() + "' in " + room.getName()
                : "Meeting requested (pending approval): '" + savedMeeting.getTitle() + "'";
        notificationService.broadcastMeetingNotification(savedMeeting, NotificationType.CONFIRMATION, msg);

        if (initialStatus == MeetingStatus.CONFIRMED) {
            List<String> attendeeEmails = savedMeeting.getAttendees().stream()
                    .map(a -> a.getUser().getEmail())
                    .filter(e -> e != null && !e.isBlank())
                    .collect(Collectors.toList());
            emailNotificationService.sendMeetingInvitation(savedMeeting, attendeeEmails);
        }

        // 7. Record Audit Log
        auditLogService.recordUserAction(
                organizer,
                AuditActionType.CREATE,
                AuditEntityType.MEETING,
                savedMeeting.getMeetingId(),
                savedMeeting.getTitle(),
                "Booked room " + room.getName() + " with " + initialStatus + " status"
        );

        return mapToResponse(savedMeeting);
    }

    public MeetingResponse updateMeeting(Long id, MeetingUpdateRequest request) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with ID: " + id));

        LocalDateTime newStart = request.getStartTime() != null ? request.getStartTime() : meeting.getStartTime();
        LocalDateTime newEnd = request.getEndTime() != null ? request.getEndTime() : meeting.getEndTime();
        Long newRoomId = request.getRoomId() != null ? request.getRoomId() : meeting.getRoom().getRoomId();

        validateMeetingTimes(newStart, newEnd);

        // Check conflict excluding current meeting
        if (meetingRepository.existsOverlappingMeetingExcluding(newRoomId, id, newStart, newEnd)) {
            throw new ConflictException("Room is already booked during the updated time interval");
        }

        if (request.getRoomId() != null && !request.getRoomId().equals(meeting.getRoom().getRoomId())) {
            Room newRoom = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + request.getRoomId()));
            meeting.setRoom(newRoom);
        }

        if (request.getTitle() != null) meeting.setTitle(request.getTitle());
        if (request.getPurpose() != null) meeting.setPurpose(request.getPurpose());
        meeting.setStartTime(newStart);
        meeting.setEndTime(newEnd);

        if (request.getStatus() != null) {
            meeting.setStatus(request.getStatus());
        }

        // Synchronize attendees if provided
        if (request.getAttendeeIds() != null) {
            List<MeetingAttendee> currentAttendees = attendeeRepository.findById_MeetingId(id);
            Set<Long> targetUserIds = new HashSet<>(request.getAttendeeIds());

            // Remove attendees no longer in the list
            for (MeetingAttendee current : currentAttendees) {
                if (!targetUserIds.contains(current.getUser().getUserId())) {
                    attendeeRepository.delete(current);
                }
            }

            Set<Long> existingUserIds = currentAttendees.stream()
                    .map(a -> a.getUser().getUserId())
                    .collect(Collectors.toSet());

            // Add newly invited attendees
            for (Long userId : targetUserIds) {
                if (!existingUserIds.contains(userId)) {
                    User attendeeUser = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("Attendee user not found with ID: " + userId));

                    String attendeeMode = systemSettingService.getString("booking.attendee_acceptance_mode", "AUTO_ACCEPT");
                    AttendeeResponseStatus attendeeStatus = "AUTO_ACCEPT".equalsIgnoreCase(attendeeMode)
                            ? AttendeeResponseStatus.ACCEPTED
                            : AttendeeResponseStatus.PENDING;

                    MeetingAttendee attendee = MeetingAttendee.builder()
                            .id(new MeetingAttendeeId(meeting.getMeetingId(), attendeeUser.getUserId()))
                            .meeting(meeting)
                            .user(attendeeUser)
                            .responseStatus(attendeeStatus)
                            .build();
                    attendeeRepository.save(attendee);

                    // Notify newly invited attendee
                    notificationService.createNotification(
                            meeting,
                            attendeeUser,
                            NotificationType.CONFIRMATION,
                            "You have been invited to meeting: '" + meeting.getTitle() + "'"
                    );
                }
            }
        }

        Meeting updated = meetingRepository.save(meeting);
        notificationService.broadcastMeetingNotification(updated, NotificationType.CHANGE, "Meeting details updated: '" + updated.getTitle() + "'");
        auditLogService.recordSystemAction(
                AuditActionType.UPDATE,
                AuditEntityType.MEETING,
                updated.getMeetingId(),
                updated.getTitle(),
                "Updated schedule or details for meeting in room " + updated.getRoom().getName()
        );
        return mapToResponse(updated);
    }

    public MeetingResponse approveMeeting(Long id) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with ID: " + id));

        if (meeting.getStatus() != MeetingStatus.PENDING) {
            throw new BadRequestException("Only PENDING meetings can be approved. Current status: " + meeting.getStatus());
        }

        meeting.setStatus(MeetingStatus.CONFIRMED);
        Meeting approved = meetingRepository.save(meeting);
        notificationService.broadcastMeetingNotification(approved, NotificationType.CONFIRMATION, "Meeting approved: '" + approved.getTitle() + "'");

        List<String> approvedEmails = attendeeRepository.findById_MeetingId(id).stream()
                .map(a -> a.getUser().getEmail())
                .filter(e -> e != null && !e.isBlank())
                .collect(Collectors.toList());
        emailNotificationService.sendMeetingInvitation(approved, approvedEmails);

        auditLogService.recordSystemAction(
                AuditActionType.APPROVE,
                AuditEntityType.MEETING,
                approved.getMeetingId(),
                approved.getTitle(),
                "Administrator approved high-capacity boardroom booking"
        );
        return mapToResponse(approved);
    }

    public MeetingResponse cancelMeeting(Long id, String reason) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with ID: " + id));

        if (meeting.getStatus() == MeetingStatus.CANCELLED) {
            throw new BadRequestException("Meeting is already cancelled");
        }

        meeting.setStatus(MeetingStatus.CANCELLED);

        // Release allocated materials back to stock
        List<MeetingMaterial> materials = meetingMaterialRepository.findById_MeetingId(id);
        for (MeetingMaterial mm : materials) {
            materialService.releaseMaterialStock(mm.getMaterial().getMaterialId(), mm.getQuantityRequested());
        }

        Meeting cancelled = meetingRepository.save(meeting);
        String cancelMsg = "Meeting cancelled: '" + cancelled.getTitle() + "'" + (reason != null ? " (Reason: " + reason + ")" : "");
        notificationService.broadcastMeetingNotification(cancelled, NotificationType.CANCELLATION, cancelMsg);

        List<String> cancelEmails = attendeeRepository.findById_MeetingId(id).stream()
                .map(a -> a.getUser().getEmail())
                .filter(e -> e != null && !e.isBlank())
                .collect(Collectors.toList());
        emailNotificationService.sendMeetingCancellation(cancelled, cancelEmails, reason);

        auditLogService.recordSystemAction(
                AuditActionType.CANCEL,
                AuditEntityType.MEETING,
                cancelled.getMeetingId(),
                cancelled.getTitle(),
                "Meeting cancelled. Reason: " + (reason != null ? reason : "No reason specified")
        );

        return mapToResponse(cancelled);
    }

    public void updateAttendeeStatus(Long meetingId, Long userId, AttendeeResponseStatus status) {
        MeetingAttendee attendee = attendeeRepository.findById_MeetingIdAndId_UserId(meetingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendee record not found for meeting: " + meetingId + " and user: " + userId));

        attendee.setResponseStatus(status);
        attendeeRepository.save(attendee);
    }

    private void validateMeetingTimes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new BadRequestException("Start time and End time must be specified");
        }
        if (!start.isBefore(end)) {
            throw new BadRequestException("Start time must be strictly before end time");
        }
    }

    public MeetingResponse checkInMeeting(Long id) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with ID: " + id));

        if (meeting.getStatus() == MeetingStatus.CANCELLED) {
            throw new BadRequestException("Cannot check in to a cancelled meeting");
        }

        meeting.setIsCheckedIn(true);
        meeting.setCheckedInAt(LocalDateTime.now());
        Meeting updated = meetingRepository.save(meeting);

        auditLogService.recordSystemAction(
                AuditActionType.UPDATE,
                AuditEntityType.MEETING,
                updated.getMeetingId(),
                updated.getTitle(),
                "Meeting presence checked in for room " + updated.getRoom().getName()
        );

        return mapToResponse(updated);
    }

    public MeetingResponse endMeetingEarly(Long id) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with ID: " + id));

        if (meeting.getStatus() == MeetingStatus.CANCELLED) {
            throw new BadRequestException("Cannot end a cancelled meeting");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(meeting.getStartTime())) {
            meeting.setEndTime(now);
        }
        meeting.setStatus(MeetingStatus.COMPLETED);
        Meeting updated = meetingRepository.save(meeting);

        // Release allocated materials back to stock
        List<MeetingMaterial> materials = meetingMaterialRepository.findById_MeetingId(id);
        for (MeetingMaterial mm : materials) {
            materialService.releaseMaterialStock(mm.getMaterial().getMaterialId(), mm.getQuantityRequested());
        }

        auditLogService.recordSystemAction(
                AuditActionType.UPDATE,
                AuditEntityType.MEETING,
                updated.getMeetingId(),
                updated.getTitle(),
                "Meeting concluded early and room " + updated.getRoom().getName() + " released"
        );

        return mapToResponse(updated);
    }

    public MeetingResponse mapToResponse(Meeting meeting) {
        List<MeetingAttendeeResponse> attendeeResponses = attendeeRepository.findById_MeetingId(meeting.getMeetingId()).stream()
                .map(a -> MeetingAttendeeResponse.builder()
                        .userId(a.getUser().getUserId())
                        .name(a.getUser().getName())
                        .email(a.getUser().getEmail())
                        .responseStatus(a.getResponseStatus())
                        .build())
                .collect(Collectors.toList());

        List<MeetingMaterialResponse> materialResponses = meetingMaterialRepository.findById_MeetingId(meeting.getMeetingId()).stream()
                .map(m -> MeetingMaterialResponse.builder()
                        .materialId(m.getMaterial().getMaterialId())
                        .name(m.getMaterial().getName())
                        .type(m.getMaterial().getType())
                        .quantityRequested(m.getQuantityRequested())
                        .build())
                .collect(Collectors.toList());

        List<MeetingStaffResponse> staffResponses = meetingStaffRepository.findById_MeetingId(meeting.getMeetingId()).stream()
                .map(s -> MeetingStaffResponse.builder()
                        .staffId(s.getStaff().getStaffId())
                        .name(s.getStaff().getName())
                        .role(s.getStaff().getRole())
                        .assignedRole(s.getAssignedRole())
                        .build())
                .collect(Collectors.toList());

        return MeetingResponse.builder()
                .meetingId(meeting.getMeetingId())
                .title(meeting.getTitle())
                .purpose(meeting.getPurpose())
                .status(meeting.getStatus())
                .startTime(meeting.getStartTime())
                .endTime(meeting.getEndTime())
                .isCheckedIn(Boolean.TRUE.equals(meeting.getIsCheckedIn()))
                .checkedInAt(meeting.getCheckedInAt())
                .createdAt(meeting.getCreatedAt())
                .updatedAt(meeting.getUpdatedAt())
                .seriesId(meeting.getRecurringSeries() != null ? meeting.getRecurringSeries().getSeriesId() : null)
                .recurrenceType(meeting.getRecurringSeries() != null ? meeting.getRecurringSeries().getRecurrenceType() : null)
                .organizer(userService.mapToDTO(meeting.getOrganizer()))
                .room(roomService.mapToResponse(meeting.getRoom()))
                .attendees(attendeeResponses)
                .materials(materialResponses)
                .staffAssignments(staffResponses)
                .build();
    }
}
