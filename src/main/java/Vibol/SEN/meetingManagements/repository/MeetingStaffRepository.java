package Vibol.SEN.meetingManagements.repository;

import Vibol.SEN.meetingManagements.model.MeetingStaff;
import Vibol.SEN.meetingManagements.model.MeetingStaffId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingStaffRepository extends JpaRepository<MeetingStaff, MeetingStaffId> {
    List<MeetingStaff> findById_MeetingId(Long meetingId);
    List<MeetingStaff> findById_StaffId(Long staffId);
    void deleteById_MeetingId(Long meetingId);
}
