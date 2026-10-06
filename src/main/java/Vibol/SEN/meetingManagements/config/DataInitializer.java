package Vibol.SEN.meetingManagements.config;

import Vibol.SEN.meetingManagements.model.*;
import Vibol.SEN.meetingManagements.model.enums.*;
import Vibol.SEN.meetingManagements.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final MaterialRepository materialRepository;
    private final StaffRepository staffRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final Vibol.SEN.meetingManagements.service.TemplateRenderService templateRenderService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedDepartments();
        seedUsers();
        seedRooms();
        seedMaterials();
        seedStaff();
        seedAuditLogs();
        seedNotificationTemplates();
        log.info("Initial database seeding check completed.");
    }

    private void seedDepartments() {
        if (departmentRepository.count() == 0) {
            departmentRepository.saveAll(List.of(
                    Department.builder().name("Information Technology").build(),
                    Department.builder().name("Human Resources").build(),
                    Department.builder().name("Finance & Operations").build(),
                    Department.builder().name("Marketing & Business Development").build()
            ));
            log.info("Default departments seeded.");
        }
    }

    private void seedUsers() {
        Department itDept = departmentRepository.findByName("Information Technology").orElse(null);
        Department hrDept = departmentRepository.findByName("Human Resources").orElse(null);

        // Ensure default Admin Vibol SEN exists with BCrypt password and Telegram credentials
        userRepository.findByEmail("vibolsen2002@gmail.com").ifPresentOrElse(
                admin -> {
                    admin.setName("Vibol SEN");
                    if (admin.getPassword() == null || !admin.getPassword().startsWith("$2a$")) {
                        admin.setPassword(passwordEncoder.encode("Vibol@2020"));
                    }
                    admin.setRole(UserRole.ADMIN);
                    if (admin.getDepartment() == null) admin.setDepartment(itDept);
                    if (admin.getTelegramChatId() == null || admin.getTelegramChatId().isBlank()) {
                        admin.setTelegramChatId("1035574371");
                    }
                    if (admin.getTelegramUsername() == null || admin.getTelegramUsername().isBlank()) {
                        admin.setTelegramUsername("vibolsen");
                    }
                    if (admin.getTelegramReminderMinutes() == null) {
                        admin.setTelegramReminderMinutes(10);
                    }
                    if (admin.getTelegramNotificationsEnabled() == null) {
                        admin.setTelegramNotificationsEnabled(true);
                    }
                    userRepository.save(admin);
                },
                () -> {
                    userRepository.save(User.builder()
                            .name("Vibol SEN")
                            .email("vibolsen2002@gmail.com")
                            .password(passwordEncoder.encode("Vibol@2020"))
                            .role(UserRole.ADMIN)
                            .department(itDept)
                            .telegramChatId("1035574371")
                            .telegramUsername("vibolsen")
                            .telegramReminderMinutes(10)
                            .telegramNotificationsEnabled(true)
                            .build());
                    log.info("Default Admin Vibol SEN seeded with BCrypt hash and Telegram Chat ID.");
                }
        );

        if (userRepository.count() <= 1) {
            userRepository.saveAll(List.of(
                    User.builder()
                            .name("Meeting Organizer")
                            .email("organizer@meeting.com")
                            .password(passwordEncoder.encode("Organizer@2020"))
                            .role(UserRole.ORGANIZER)
                            .department(hrDept)
                            .build(),
                    User.builder()
                            .name("Alice Johnson")
                            .email("alice@meeting.com")
                            .password(passwordEncoder.encode("Alice@2020"))
                            .role(UserRole.EMPLOYEE)
                            .department(itDept)
                            .build()
            ));
            log.info("Default users seeded with BCrypt hashes.");
        }
    }

    private void seedRooms() {
        if (roomRepository.count() == 0) {
            roomRepository.saveAll(List.of(
                    Room.builder()
                            .name("Executive Boardroom")
                            .location("Building A, 4th Floor")
                            .capacity(20)
                            .status(RoomStatus.ACTIVE)
                            .build(),
                    Room.builder()
                            .name("Conference Hall Orion")
                            .location("Building A, 2nd Floor")
                            .capacity(12)
                            .status(RoomStatus.ACTIVE)
                            .build(),
                    Room.builder()
                            .name("Huddle Room 1")
                            .location("Building B, 1st Floor")
                            .capacity(6)
                            .status(RoomStatus.ACTIVE)
                            .build(),
                    Room.builder()
                            .name("Innovation Lab")
                            .location("Building C, 3rd Floor")
                            .capacity(30)
                            .status(RoomStatus.ACTIVE)
                            .build()
            ));
            log.info("Default rooms seeded.");
        }
    }

    private void seedMaterials() {
        if (materialRepository.count() == 0) {
            materialRepository.saveAll(List.of(
                    Material.builder()
                            .name("4K Laser Projector")
                            .type(MaterialType.EQUIPMENT)
                            .quantityAvailable(6)
                            .build(),
                    Material.builder()
                            .name("Interactive Digital Whiteboard")
                            .type(MaterialType.EQUIPMENT)
                            .quantityAvailable(4)
                            .build(),
                    Material.builder()
                            .name("Wireless Conference Speakerphone")
                            .type(MaterialType.EQUIPMENT)
                            .quantityAvailable(10)
                            .build(),
                    Material.builder()
                            .name("Flipchart & Marker Set")
                            .type(MaterialType.STATIONERY)
                            .quantityAvailable(25)
                            .build(),
                    Material.builder()
                            .name("Coffee & Refreshment Package")
                            .type(MaterialType.CATERING)
                            .quantityAvailable(100)
                            .build()
            ));
            log.info("Default materials seeded.");
        }
    }

    private void seedStaff() {
        if (staffRepository.count() == 0) {
            staffRepository.saveAll(List.of(
                    Staff.builder()
                            .name("John Miller")
                            .role(StaffRole.TECHNICIAN)
                            .skill("Audio/Visual & Video Conference Systems")
                            .availabilityStatus(StaffAvailability.AVAILABLE)
                            .build(),
                    Staff.builder()
                            .name("Sophia Davis")
                            .role(StaffRole.RECEPTIONIST)
                            .skill("Guest Check-in & Room Preparation")
                            .availabilityStatus(StaffAvailability.AVAILABLE)
                            .build(),
                    Staff.builder()
                            .name("Michael Chang")
                            .role(StaffRole.FACILITATOR)
                            .skill("Meeting Moderation & Timekeeping")
                            .availabilityStatus(StaffAvailability.AVAILABLE)
                            .build()
            ));
            log.info("Default support staff seeded.");
        }
    }

    private void seedAuditLogs() {
        if (auditLogRepository.count() == 0) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            auditLogRepository.saveAll(List.of(
                    AuditLog.builder()
                            .actorName("System Engine")
                            .actorEmail("system@meeting.internal")
                            .actionType(AuditActionType.CREATE)
                            .entityType(AuditEntityType.SYSTEM)
                            .entityName("MMS Application Core")
                            .details("Initialized Meeting Management System database schemas and default departments")
                            .ipAddress("127.0.0.1")
                            .createdAt(now.minusHours(48))
                            .build(),
                    AuditLog.builder()
                            .actorName("Vibol SEN")
                            .actorEmail("vibolsen2002@gmail.com")
                            .actionType(AuditActionType.LOGIN)
                            .entityType(AuditEntityType.USER)
                            .entityName("Administrator Session")
                            .details("SuperAdmin authentication via Spring Boot Security JWT token")
                            .ipAddress("192.168.1.105")
                            .createdAt(now.minusHours(36))
                            .build(),
                    AuditLog.builder()
                            .actorName("Vibol SEN")
                            .actorEmail("vibolsen2002@gmail.com")
                            .actionType(AuditActionType.CREATE)
                            .entityType(AuditEntityType.ROOM)
                            .entityId(1L)
                            .entityName("Executive Boardroom A")
                            .details("Configured high-capacity facility (20 pax) with 4K Video Bar and AV matrix")
                            .ipAddress("192.168.1.105")
                            .createdAt(now.minusHours(24))
                            .build(),
                    AuditLog.builder()
                            .actorName("Meeting Organizer")
                            .actorEmail("organizer@meeting.com")
                            .actionType(AuditActionType.CREATE)
                            .entityType(AuditEntityType.MEETING)
                            .entityId(101L)
                            .entityName("Q4 Strategic Planning")
                            .details("Requested Executive Boardroom A with 22 attendees; submitted for Admin approval")
                            .ipAddress("192.168.1.142")
                            .createdAt(now.minusHours(12))
                            .build(),
                    AuditLog.builder()
                            .actorName("Vibol SEN")
                            .actorEmail("vibolsen2002@gmail.com")
                            .actionType(AuditActionType.APPROVE)
                            .entityType(AuditEntityType.MEETING)
                            .entityId(101L)
                            .entityName("Q4 Strategic Planning")
                            .details("Approved boardroom capacity exception for executive stakeholder alignment")
                            .ipAddress("192.168.1.105")
                            .createdAt(now.minusHours(10))
                            .build(),
                    AuditLog.builder()
                            .actorName("Alice Johnson")
                            .actorEmail("alice@meeting.com")
                            .actionType(AuditActionType.ROLE_CHANGE)
                            .entityType(AuditEntityType.USER)
                            .entityId(3L)
                            .entityName("Alice Johnson")
                            .details("Assigned Standard Employee role in Marketing & Business Development")
                            .ipAddress("192.168.1.180")
                            .createdAt(now.minusHours(6))
                            .build()
            ));
            log.info("Default audit logs seeded.");
        }
    }

    private void seedNotificationTemplates() {
        if (notificationTemplateRepository.count() == 0) {
            notificationTemplateRepository.saveAll(List.of(
                    NotificationTemplate.builder()
                            .type(NotificationType.REMINDER)
                            .name("Pre-Meeting Countdown Reminder")
                            .content(templateRenderService.getDefaultTemplate(NotificationType.REMINDER))
                            .isCustomized(false)
                            .build(),
                    NotificationTemplate.builder()
                            .type(NotificationType.CONFIRMATION)
                            .name("Meeting Creation & Confirmation")
                            .content(templateRenderService.getDefaultTemplate(NotificationType.CONFIRMATION))
                            .isCustomized(false)
                            .build(),
                    NotificationTemplate.builder()
                            .type(NotificationType.CHANGE)
                            .name("Meeting Approval & Modification")
                            .content(templateRenderService.getDefaultTemplate(NotificationType.CHANGE))
                            .isCustomized(false)
                            .build(),
                    NotificationTemplate.builder()
                            .type(NotificationType.CANCELLATION)
                            .name("Meeting Cancellation")
                            .content(templateRenderService.getDefaultTemplate(NotificationType.CANCELLATION))
                            .isCustomized(false)
                            .build()
            ));
            log.info("Default Telegram notification templates seeded.");
        }
    }
}
