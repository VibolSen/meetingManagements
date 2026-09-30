package Vibol.SEN.meetingManagements.service;

import Vibol.SEN.meetingManagements.dto.StaffRequest;
import Vibol.SEN.meetingManagements.dto.StaffResponse;
import Vibol.SEN.meetingManagements.exception.BadRequestException;
import Vibol.SEN.meetingManagements.exception.ResourceNotFoundException;
import Vibol.SEN.meetingManagements.model.Staff;
import Vibol.SEN.meetingManagements.model.enums.StaffRole;
import Vibol.SEN.meetingManagements.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StaffService {

    private final StaffRepository staffRepository;

    @Transactional(readOnly = true)
    public List<StaffResponse> getAllStaff() {
        return staffRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StaffResponse getStaffById(Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found with ID: " + id));
        return mapToResponse(staff);
    }

    @Transactional(readOnly = true)
    public List<StaffResponse> getStaffByRole(StaffRole role) {
        return staffRepository.findByRole(role).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public StaffResponse createStaff(StaffRequest request) {
        Staff staff = Staff.builder()
                .name(request.getName())
                .role(request.getRole())
                .skill(request.getSkill())
                .availabilityStatus(request.getAvailabilityStatus())
                .build();
        Staff saved = staffRepository.save(staff);
        return mapToResponse(saved);
    }

    public StaffResponse updateStaff(Long id, StaffRequest request) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found with ID: " + id));

        staff.setName(request.getName());
        staff.setRole(request.getRole());
        staff.setSkill(request.getSkill());
        staff.setAvailabilityStatus(request.getAvailabilityStatus());

        Staff updated = staffRepository.save(staff);
        return mapToResponse(updated);
    }

    public void deleteStaff(Long id) {
        if (!staffRepository.existsById(id)) {
            throw new ResourceNotFoundException("Staff not found with ID: " + id);
        }
        staffRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<StaffResponse> getAvailableStaffForTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end) || start.isEqual(end)) {
            throw new BadRequestException("Start time must be before end time");
        }
        return staffRepository.findAvailableStaffForTimeRange(start, end).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public StaffResponse mapToResponse(Staff staff) {
        return StaffResponse.builder()
                .staffId(staff.getStaffId())
                .name(staff.getName())
                .role(staff.getRole())
                .skill(staff.getSkill())
                .availabilityStatus(staff.getAvailabilityStatus())
                .build();
    }
}
