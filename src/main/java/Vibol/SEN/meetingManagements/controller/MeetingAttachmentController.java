package Vibol.SEN.meetingManagements.controller;

import Vibol.SEN.meetingManagements.dto.MeetingAttachmentCreateRequest;
import Vibol.SEN.meetingManagements.dto.MeetingAttachmentDTO;
import Vibol.SEN.meetingManagements.service.MeetingAttachmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MeetingAttachmentController {

    private final MeetingAttachmentService attachmentService;

    @GetMapping("/meetings/{meetingId}/attachments")
    public ResponseEntity<List<MeetingAttachmentDTO>> getAttachments(@PathVariable Long meetingId) {
        return ResponseEntity.ok(attachmentService.getByMeeting(meetingId));
    }

    @PostMapping("/meetings/{meetingId}/attachments")
    public ResponseEntity<MeetingAttachmentDTO> addAttachment(
            @PathVariable Long meetingId,
            @Valid @RequestBody MeetingAttachmentCreateRequest request) {
        return new ResponseEntity<>(attachmentService.addAttachment(meetingId, request), HttpStatus.CREATED);
    }

    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long attachmentId) {
        attachmentService.deleteAttachment(attachmentId);
        return ResponseEntity.noContent().build();
    }
}
