package Vibol.SEN.meetingManagements.controller;

import Vibol.SEN.meetingManagements.dto.NotificationResponse;
import Vibol.SEN.meetingManagements.model.enums.NotificationStatus;
import Vibol.SEN.meetingManagements.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationResponse>> getUserNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id, @RequestParam NotificationStatus status) {
        notificationService.markNotificationStatus(id, status);
        return ResponseEntity.ok().build();
    }
}
