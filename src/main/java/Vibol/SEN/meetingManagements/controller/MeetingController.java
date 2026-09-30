package Vibol.SEN.meetingManagements.controller;

import Vibol.SEN.meetingManagements.dto.MeetingCreateRequest;
import Vibol.SEN.meetingManagements.dto.MeetingResponse;
import Vibol.SEN.meetingManagements.dto.MeetingUpdateRequest;
import Vibol.SEN.meetingManagements.model.enums.AttendeeResponseStatus;
import Vibol.SEN.meetingManagements.model.enums.MeetingStatus;
import Vibol.SEN.meetingManagements.service.MeetingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    @GetMapping
    public ResponseEntity<List<MeetingResponse>> getAllMeetings() {
        return ResponseEntity.ok(meetingService.getAllMeetings());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MeetingResponse> getMeetingById(@PathVariable Long id) {
        return ResponseEntity.ok(meetingService.getMeetingById(id));
    }

    @GetMapping("/organizer/{organizerId}")
    public ResponseEntity<List<MeetingResponse>> getMeetingsByOrganizer(@PathVariable Long organizerId) {
        return ResponseEntity.ok(meetingService.getMeetingsByOrganizer(organizerId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<MeetingResponse>> getMeetingsByStatus(@PathVariable MeetingStatus status) {
        return ResponseEntity.ok(meetingService.getMeetingsByStatus(status));
    }

    @GetMapping("/calendar")
    public ResponseEntity<List<MeetingResponse>> getMeetingsForCalendar(
            @RequestParam(required = false) Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(meetingService.getMeetingsForCalendar(roomId, start, end));
    }

    @PostMapping
    public ResponseEntity<MeetingResponse> createMeeting(@Valid @RequestBody MeetingCreateRequest request) {
        return new ResponseEntity<>(meetingService.createMeeting(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MeetingResponse> updateMeeting(@PathVariable Long id, @Valid @RequestBody MeetingUpdateRequest request) {
        return ResponseEntity.ok(meetingService.updateMeeting(id, request));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<MeetingResponse> approveMeeting(@PathVariable Long id) {
        return ResponseEntity.ok(meetingService.approveMeeting(id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<MeetingResponse> cancelMeeting(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(meetingService.cancelMeeting(id, reason));
    }

    @PatchMapping("/{id}/attendee/{userId}/rsvp")
    public ResponseEntity<Void> updateAttendeeRsvp(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestParam AttendeeResponseStatus status) {
        meetingService.updateAttendeeStatus(id, userId, status);
        return ResponseEntity.ok().build();
    }
}
