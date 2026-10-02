package Vibol.SEN.meetingManagements.repository;

import Vibol.SEN.meetingManagements.model.User;
import Vibol.SEN.meetingManagements.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(UserRole role);
    List<User> findByDepartment_DepartmentId(Long departmentId);
    long countByDepartment_DepartmentId(Long departmentId);
}
