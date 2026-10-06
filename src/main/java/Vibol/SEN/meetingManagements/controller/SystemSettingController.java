package Vibol.SEN.meetingManagements.controller;

import Vibol.SEN.meetingManagements.dto.SystemSettingResponse;
import Vibol.SEN.meetingManagements.dto.SystemSettingUpdateRequest;
import Vibol.SEN.meetingManagements.model.enums.SettingCategory;
import Vibol.SEN.meetingManagements.service.SystemSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/settings")
@RequiredArgsConstructor
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    @GetMapping
    public ResponseEntity<List<SystemSettingResponse>> getPublicSettings() {
        return ResponseEntity.ok(systemSettingService.getPublicSettings());
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SystemSettingResponse>> getAllSettings() {
        return ResponseEntity.ok(systemSettingService.getAllSettings());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<SystemSettingResponse>> getByCategory(@PathVariable SettingCategory category) {
        return ResponseEntity.ok(systemSettingService.getSettingsByCategory(category));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SystemSettingResponse>> batchUpdate(
            @Valid @RequestBody List<SystemSettingUpdateRequest> updates,
            Authentication authentication
    ) {
        String author = authentication != null ? authentication.getName() : "ADMIN";
        return ResponseEntity.ok(systemSettingService.batchUpdate(updates, author));
    }

    @PatchMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemSettingResponse> updateSetting(
            @PathVariable String key,
            @RequestParam String value,
            Authentication authentication
    ) {
        String author = authentication != null ? authentication.getName() : "ADMIN";
        return ResponseEntity.ok(systemSettingService.updateSetting(key, value, author));
    }

    @PostMapping("/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resetToDefaults(Authentication authentication) {
        String author = authentication != null ? authentication.getName() : "ADMIN";
        systemSettingService.resetToDefaults(author);
        return ResponseEntity.noContent().build();
    }
}
