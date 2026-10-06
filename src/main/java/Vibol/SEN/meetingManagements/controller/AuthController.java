package Vibol.SEN.meetingManagements.controller;

import Vibol.SEN.meetingManagements.dto.*;
import Vibol.SEN.meetingManagements.exception.BadRequestException;
import Vibol.SEN.meetingManagements.exception.ResourceNotFoundException;
import Vibol.SEN.meetingManagements.model.Department;
import Vibol.SEN.meetingManagements.model.User;
import Vibol.SEN.meetingManagements.repository.DepartmentRepository;
import Vibol.SEN.meetingManagements.repository.UserRepository;
import Vibol.SEN.meetingManagements.security.JwtTokenProvider;
import Vibol.SEN.meetingManagements.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }

        if (user.getStatus() == Vibol.SEN.meetingManagements.model.enums.UserStatus.SUSPENDED) {
            throw new BadRequestException("This account has been suspended. Please contact your system administrator.");
        }

        String token = tokenProvider.generateToken(user.getUserId(), user.getEmail(), user.getRole());
        UserDTO userDTO = userService.getUserById(user.getUserId());

        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(userDTO)
                .build());
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        throw new BadRequestException("Public account registration is disabled. Company accounts must be provisioned by a system administrator.");
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("User is not authenticated");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return ResponseEntity.ok(userService.getUserById(user.getUserId()));
    }
}
