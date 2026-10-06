package Vibol.SEN.meetingManagements.controller;

import Vibol.SEN.meetingManagements.dto.MeetingMinutesDTO;
import Vibol.SEN.meetingManagements.dto.MeetingMinutesRequest;
import Vibol.SEN.meetingManagements.service.MeetingMinutesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meetings/{meetingId}/minutes")
@RequiredArgsConstructor
public class MeetingMinutesController {

    private final MeetingMinutesService meetingMinutesService;

    @GetMapping
    public ResponseEntity<MeetingMinutesDTO> getMinutes(@PathVariable Long meetingId) {
        MeetingMinutesDTO minutes = meetingMinutesService.getMinutes(meetingId);
        return minutes != null ? ResponseEntity.ok(minutes) : ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<MeetingMinutesDTO> saveMinutes(
            @PathVariable Long meetingId,
            @Valid @RequestBody MeetingMinutesRequest request) {
        return ResponseEntity.ok(meetingMinutesService.saveOrUpdateMinutes(meetingId, request));
    }
}
