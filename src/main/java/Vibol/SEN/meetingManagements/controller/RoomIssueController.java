package Vibol.SEN.meetingManagements.controller;

import Vibol.SEN.meetingManagements.dto.RoomIssueCreateRequest;
import Vibol.SEN.meetingManagements.dto.RoomIssueDTO;
import Vibol.SEN.meetingManagements.dto.RoomIssueUpdateRequest;
import Vibol.SEN.meetingManagements.service.RoomIssueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RoomIssueController {

    private final RoomIssueService roomIssueService;

    @GetMapping("/rooms/{roomId}/issues")
    public ResponseEntity<List<RoomIssueDTO>> getIssuesForRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(roomIssueService.getIssuesForRoom(roomId));
    }

    @PostMapping("/rooms/{roomId}/issues")
    public ResponseEntity<RoomIssueDTO> reportIssue(
            @PathVariable Long roomId,
            @Valid @RequestBody RoomIssueCreateRequest request,
            @RequestParam(required = false) Long userId) {
        return new ResponseEntity<>(roomIssueService.reportIssue(roomId, request, userId), HttpStatus.CREATED);
    }

    @GetMapping("/issues")
    public ResponseEntity<List<RoomIssueDTO>> getAllIssues() {
        return ResponseEntity.ok(roomIssueService.getAllIssues());
    }

    @PatchMapping("/issues/{issueId}/status")
    public ResponseEntity<RoomIssueDTO> updateIssueStatus(
            @PathVariable Long issueId,
            @Valid @RequestBody RoomIssueUpdateRequest request) {
        return ResponseEntity.ok(roomIssueService.updateIssueStatus(issueId, request));
    }
}
