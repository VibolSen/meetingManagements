package Vibol.SEN.meetingManagements.service;

import Vibol.SEN.meetingManagements.dto.DepartmentDTO;
import Vibol.SEN.meetingManagements.exception.BadRequestException;
import Vibol.SEN.meetingManagements.exception.ResourceNotFoundException;
import Vibol.SEN.meetingManagements.model.Department;
import Vibol.SEN.meetingManagements.model.User;
import Vibol.SEN.meetingManagements.repository.DepartmentRepository;
import Vibol.SEN.meetingManagements.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<DepartmentDTO> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DepartmentDTO getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));
        return mapToDTO(department);
    }

    public DepartmentDTO createDepartment(DepartmentDTO dto) {
        String cleanName = dto.getName().trim();
        if (departmentRepository.existsByName(cleanName)) {
            throw new BadRequestException("Department with name '" + cleanName + "' already exists");
        }
        Department department = Department.builder()
                .name(cleanName)
                .description(dto.getDescription() != null ? dto.getDescription().trim() : null)
                .build();
        Department saved = departmentRepository.save(department);
        return mapToDTO(saved);
    }

    public DepartmentDTO updateDepartment(Long id, DepartmentDTO dto) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));

        String cleanName = dto.getName().trim();
        departmentRepository.findByName(cleanName).ifPresent(existing -> {
            if (!existing.getDepartmentId().equals(id)) {
                throw new BadRequestException("Department with name '" + cleanName + "' already exists");
            }
        });

        department.setName(cleanName);
        if (dto.getDescription() != null) {
            department.setDescription(dto.getDescription().trim());
        }
        Department updated = departmentRepository.save(department);
        return mapToDTO(updated);
    }

    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));

        // Safely disassociate all assigned users before removing department
        List<User> assignedUsers = userRepository.findByDepartment_DepartmentId(id);
        for (User user : assignedUsers) {
            user.setDepartment(null);
            userRepository.save(user);
        }

        departmentRepository.delete(department);
    }

    public DepartmentDTO mapToDTO(Department department) {
        long count = userRepository.countByDepartment_DepartmentId(department.getDepartmentId());
        return DepartmentDTO.builder()
                .departmentId(department.getDepartmentId())
                .name(department.getName())
                .description(department.getDescription())
                .memberCount((int) count)
                .build();
    }
}
