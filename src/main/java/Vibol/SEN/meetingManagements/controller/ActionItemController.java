package Vibol.SEN.meetingManagements.controller;

import Vibol.SEN.meetingManagements.dto.ActionItemCreateRequest;
import Vibol.SEN.meetingManagements.dto.ActionItemDTO;
import Vibol.SEN.meetingManagements.model.enums.ActionItemStatus;
import Vibol.SEN.meetingManagements.service.ActionItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ActionItemController {

    private final ActionItemService actionItemService;

    @GetMapping("/meetings/{meetingId}/actions")
    public ResponseEntity<List<ActionItemDTO>> getMeetingActionItems(@PathVariable Long meetingId) {
        return ResponseEntity.ok(actionItemService.getByMeeting(meetingId));
    }

    @PostMapping("/meetings/{meetingId}/actions")
    public ResponseEntity<ActionItemDTO> createActionItem(
            @PathVariable Long meetingId,
            @Valid @RequestBody ActionItemCreateRequest request) {
        return new ResponseEntity<>(actionItemService.createActionItem(meetingId, request), HttpStatus.CREATED);
    }

    @GetMapping("/actions/user/{userId}")
    public ResponseEntity<List<ActionItemDTO>> getUserActionItems(
            @PathVariable Long userId,
            @RequestParam(required = false) ActionItemStatus status) {
        return ResponseEntity.ok(actionItemService.getByUser(userId, status));
    }

    @PatchMapping("/actions/{itemId}/status")
    public ResponseEntity<ActionItemDTO> updateActionItemStatus(
            @PathVariable Long itemId,
            @RequestParam ActionItemStatus status) {
        return ResponseEntity.ok(actionItemService.updateStatus(itemId, status));
    }

    @DeleteMapping("/actions/{itemId}")
    public ResponseEntity<Void> deleteActionItem(@PathVariable Long itemId) {
        actionItemService.deleteActionItem(itemId);
        return ResponseEntity.noContent().build();
    }
}
