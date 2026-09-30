package Vibol.SEN.meetingManagements.service;

import Vibol.SEN.meetingManagements.dto.DepartmentDTO;
import Vibol.SEN.meetingManagements.exception.BadRequestException;
import Vibol.SEN.meetingManagements.exception.ResourceNotFoundException;
import Vibol.SEN.meetingManagements.model.Department;
import Vibol.SEN.meetingManagements.repository.DepartmentRepository;
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
        if (departmentRepository.existsByName(dto.getName())) {
            throw new BadRequestException("Department with name '" + dto.getName() + "' already exists");
        }
        Department department = Department.builder()
                .name(dto.getName())
                .build();
        Department saved = departmentRepository.save(department);
        return mapToDTO(saved);
    }

    public void deleteDepartment(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Department not found with ID: " + id);
        }
        departmentRepository.deleteById(id);
    }

    public DepartmentDTO mapToDTO(Department department) {
        return DepartmentDTO.builder()
                .departmentId(department.getDepartmentId())
                .name(department.getName())
                .build();
    }
}
