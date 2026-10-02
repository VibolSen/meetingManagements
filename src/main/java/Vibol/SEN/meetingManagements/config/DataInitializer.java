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
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedDepartments();
        seedUsers();
        seedRooms();
        seedMaterials();
        seedStaff();
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

        // Ensure default Admin Vibol SEN exists with BCrypt password
        userRepository.findByEmail("vibolsen2002@gmail.com").ifPresentOrElse(
                admin -> {
                    admin.setName("Vibol SEN");
                    if (admin.getPassword() == null || !admin.getPassword().startsWith("$2a$")) {
                        admin.setPassword(passwordEncoder.encode("Vibol@2020"));
                    }
                    admin.setRole(UserRole.ADMIN);
                    if (admin.getDepartment() == null) admin.setDepartment(itDept);
                    userRepository.save(admin);
                },
                () -> {
                    userRepository.save(User.builder()
                            .name("Vibol SEN")
                            .email("vibolsen2002@gmail.com")
                            .password(passwordEncoder.encode("Vibol@2020"))
                            .role(UserRole.ADMIN)
                            .department(itDept)
                            .build());
                    log.info("Default Admin Vibol SEN seeded with BCrypt hash.");
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
}
