package Vibol.SEN.meetingManagements.repository;

import Vibol.SEN.meetingManagements.model.Staff;
import Vibol.SEN.meetingManagements.model.enums.StaffAvailability;
import Vibol.SEN.meetingManagements.model.enums.StaffRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    List<Staff> findByRole(StaffRole role);

    List<Staff> findByAvailabilityStatus(StaffAvailability availabilityStatus);

    List<Staff> findByRoleAndAvailabilityStatus(StaffRole role, StaffAvailability availabilityStatus);

    /**
     * Finds staff members who are marked AVAILABLE and not currently assigned to another active meeting overlapping [startTime, endTime).
     */
    @Query("SELECT s FROM Staff s " +
           "WHERE s.availabilityStatus = 'AVAILABLE' " +
           "AND s.staffId NOT IN (" +
           "   SELECT ms.staff.staffId FROM MeetingStaff ms " +
           "   WHERE ms.meeting.status != 'CANCELLED' " +
           "   AND ms.meeting.startTime < :endTime " +
           "   AND ms.meeting.endTime > :startTime" +
           ")")
    List<Staff> findAvailableStaffForTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
