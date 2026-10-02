package Vibol.SEN.meetingManagements.service;

import Vibol.SEN.meetingManagements.dto.UserCreateRequest;
import Vibol.SEN.meetingManagements.dto.UserDTO;
import Vibol.SEN.meetingManagements.dto.UserUpdateRequest;
import Vibol.SEN.meetingManagements.exception.BadRequestException;
import Vibol.SEN.meetingManagements.exception.ResourceNotFoundException;
import Vibol.SEN.meetingManagements.model.Department;
import Vibol.SEN.meetingManagements.model.User;
import Vibol.SEN.meetingManagements.model.enums.UserRole;
import Vibol.SEN.meetingManagements.model.enums.UserStatus;
import Vibol.SEN.meetingManagements.repository.DepartmentRepository;
import Vibol.SEN.meetingManagements.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        return mapToDTO(user);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getUsersByRole(UserRole role) {
        return userRepository.findByRole(role).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public UserDTO createUser(UserCreateRequest request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new BadRequestException("User with email '" + cleanEmail + "' already exists");
        }

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + request.getDepartmentId()));
        }

        String encodedPassword = null;
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            encodedPassword = passwordEncoder.encode(request.getPassword().trim());
        }

        UserStatus status = request.getStatus() != null ? request.getStatus() : UserStatus.ACTIVE;

        User user = User.builder()
                .name(request.getName().trim())
                .email(cleanEmail)
                .password(encodedPassword)
                .role(request.getRole())
                .status(status)
                .avatarUrl(request.getAvatarUrl() != null ? request.getAvatarUrl().trim() : null)
                .department(department)
                .build();

        User saved = userRepository.save(user);
        return mapToDTO(saved);
    }

    public UserDTO updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        String cleanEmail = request.getEmail().trim().toLowerCase();
        userRepository.findByEmail(cleanEmail).ifPresent(existing -> {
            if (!existing.getUserId().equals(id)) {
                throw new BadRequestException("User with email '" + cleanEmail + "' already exists");
            }
        });

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + request.getDepartmentId()));
        }

        user.setName(request.getName().trim());
        user.setEmail(cleanEmail);
        user.setRole(request.getRole());
        user.setDepartment(department);

        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        }

        User updated = userRepository.save(user);
        return mapToDTO(updated);
    }

    public UserDTO updateStatus(Long id, UserStatus status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        user.setStatus(status);
        User saved = userRepository.save(user);
        return mapToDTO(saved);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with ID: " + id);
        }
        userRepository.deleteById(id);
    }

    public UserDTO mapToDTO(User user) {
        return UserDTO.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus() != null ? user.getStatus() : UserStatus.ACTIVE)
                .avatarUrl(user.getAvatarUrl())
                .departmentId(user.getDepartment() != null ? user.getDepartment().getDepartmentId() : null)
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .build();
    }
}
