package Vibol.SEN.meetingManagements.service;

import Vibol.SEN.meetingManagements.dto.UserCreateRequest;
import Vibol.SEN.meetingManagements.dto.UserDTO;
import Vibol.SEN.meetingManagements.dto.UserUpdateRequest;
import Vibol.SEN.meetingManagements.exception.BadRequestException;
import Vibol.SEN.meetingManagements.exception.ResourceNotFoundException;
import Vibol.SEN.meetingManagements.model.Department;
import Vibol.SEN.meetingManagements.model.User;
import Vibol.SEN.meetingManagements.model.enums.BookingAccessLevel;
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
        Vibol.SEN.meetingManagements.model.enums.BookingAccessLevel access = request.getBookingAccess() != null
                ? request.getBookingAccess()
                : Vibol.SEN.meetingManagements.model.enums.BookingAccessLevel.FULL_ACCESS;

        User user = User.builder()
                .name(request.getName().trim())
                .email(cleanEmail)
                .password(encodedPassword)
                .role(request.getRole())
                .status(status)
                .bookingAccess(access)
                .avatarUrl(request.getAvatarUrl() != null ? request.getAvatarUrl().trim() : null)
                .phone(request.getPhone() != null && !request.getPhone().trim().isEmpty() ? request.getPhone().trim() : null)
                .jobTitle(request.getJobTitle() != null && !request.getJobTitle().trim().isEmpty() ? request.getJobTitle().trim() : null)
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

        if (request.getBookingAccess() != null) {
            user.setBookingAccess(request.getBookingAccess());
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim().isEmpty() ? null : request.getPhone().trim());
        }

        if (request.getJobTitle() != null) {
            user.setJobTitle(request.getJobTitle().trim().isEmpty() ? null : request.getJobTitle().trim());
        }

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        }

        if (request.getTelegramChatId() != null) {
            user.setTelegramChatId(request.getTelegramChatId().trim());
        }
        if (request.getTelegramUsername() != null) {
            user.setTelegramUsername(request.getTelegramUsername().trim());
        }
        if (request.getTelegramReminderMinutes() != null) {
            user.setTelegramReminderMinutes(request.getTelegramReminderMinutes());
        }
        if (request.getTelegramNotificationsEnabled() != null) {
            user.setTelegramNotificationsEnabled(request.getTelegramNotificationsEnabled());
        }

        User updated = userRepository.save(user);
        return mapToDTO(updated);
    }

    public UserDTO updateTelegramSettings(Long id, Vibol.SEN.meetingManagements.dto.TelegramSettingsRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        if (request.getTelegramChatId() != null) {
            user.setTelegramChatId(request.getTelegramChatId().trim());
        }
        if (request.getTelegramUsername() != null) {
            user.setTelegramUsername(request.getTelegramUsername().trim());
        }
        if (request.getTelegramReminderMinutes() != null) {
            user.setTelegramReminderMinutes(request.getTelegramReminderMinutes());
        }
        if (request.getTelegramNotificationsEnabled() != null) {
            user.setTelegramNotificationsEnabled(request.getTelegramNotificationsEnabled());
        }

        User saved = userRepository.save(user);
        return mapToDTO(saved);
    }

    public UserDTO updateStatus(Long id, UserStatus status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        user.setStatus(status);
        User saved = userRepository.save(user);
        return mapToDTO(saved);
    }

    public UserDTO updateBookingAccess(Long id, BookingAccessLevel access) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        user.setBookingAccess(access != null ? access : BookingAccessLevel.FULL_ACCESS);
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
                .bookingAccess(user.getBookingAccess() != null ? user.getBookingAccess() : BookingAccessLevel.FULL_ACCESS)
                .avatarUrl(user.getAvatarUrl())
                .phone(user.getPhone())
                .jobTitle(user.getJobTitle())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .departmentId(user.getDepartment() != null ? user.getDepartment().getDepartmentId() : null)
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .telegramChatId(user.getTelegramChatId())
                .telegramUsername(user.getTelegramUsername())
                .telegramReminderMinutes(user.getTelegramReminderMinutes() != null ? user.getTelegramReminderMinutes() : 10)
                .telegramNotificationsEnabled(user.getTelegramNotificationsEnabled() != null ? user.getTelegramNotificationsEnabled() : true)
                .build();
    }
}
