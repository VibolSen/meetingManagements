package Vibol.SEN.meetingManagements.controller;

import Vibol.SEN.meetingManagements.dto.StaffRequest;
import Vibol.SEN.meetingManagements.dto.StaffResponse;
import Vibol.SEN.meetingManagements.model.enums.StaffRole;
import Vibol.SEN.meetingManagements.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    public ResponseEntity<List<StaffResponse>> getAllStaff() {
        return ResponseEntity.ok(staffService.getAllStaff());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StaffResponse> getStaffById(@PathVariable Long id) {
        return ResponseEntity.ok(staffService.getStaffById(id));
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<List<StaffResponse>> getStaffByRole(@PathVariable StaffRole role) {
        return ResponseEntity.ok(staffService.getStaffByRole(role));
    }

    @GetMapping("/available")
    public ResponseEntity<List<StaffResponse>> getAvailableStaff(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(staffService.getAvailableStaffForTimeRange(start, end));
    }

    @PostMapping
    public ResponseEntity<StaffResponse> createStaff(@Valid @RequestBody StaffRequest request) {
        return new ResponseEntity<>(staffService.createStaff(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StaffResponse> updateStaff(@PathVariable Long id, @Valid @RequestBody StaffRequest request) {
        return ResponseEntity.ok(staffService.updateStaff(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStaff(@PathVariable Long id) {
        staffService.deleteStaff(id);
        return ResponseEntity.noContent().build();
    }
}
