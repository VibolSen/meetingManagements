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

import java.time.LocalDateTime;
import java.util.List;
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

        // 2. Approval workflow: High-capacity rooms (>= 20) require approval, others auto-confirm
        MeetingStatus initialStatus = (room.getCapacity() >= 20) ? MeetingStatus.PENDING : MeetingStatus.CONFIRMED;

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

        // 3. Attach attendees
        if (request.getAttendeeIds() != null) {
            for (Long userId : request.getAttendeeIds()) {
                User attendeeUser = userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Attendee user not found with ID: " + userId));

                MeetingAttendee attendee = MeetingAttendee.builder()
                        .id(new MeetingAttendeeId(savedMeeting.getMeetingId(), attendeeUser.getUserId()))
                        .meeting(savedMeeting)
                        .user(attendeeUser)
                        .responseStatus(AttendeeResponseStatus.PENDING)
                        .build();
                attendeeRepository.save(attendee);
                savedMeeting.getAttendees().add(attendee);
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
                meetingMaterialRepository.save(mm);
                savedMeeting.getMaterials().add(mm);
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
                meetingStaffRepository.save(ms);
                savedMeeting.getStaffAssignments().add(ms);
            }
        }

        // 6. Broadcast notification
        String msg = initialStatus == MeetingStatus.CONFIRMED
                ? "Meeting confirmed: '" + savedMeeting.getTitle() + "' in " + room.getName()
                : "Meeting requested (pending approval): '" + savedMeeting.getTitle() + "'";
        notificationService.broadcastMeetingNotification(savedMeeting, NotificationType.CONFIRMATION, msg);

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

        Meeting updated = meetingRepository.save(meeting);
        notificationService.broadcastMeetingNotification(updated, NotificationType.CHANGE, "Meeting details updated: '" + updated.getTitle() + "'");
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
                .createdAt(meeting.getCreatedAt())
                .updatedAt(meeting.getUpdatedAt())
                .organizer(userService.mapToDTO(meeting.getOrganizer()))
                .room(roomService.mapToResponse(meeting.getRoom()))
                .attendees(attendeeResponses)
                .materials(materialResponses)
                .staffAssignments(staffResponses)
                .build();
    }
}
