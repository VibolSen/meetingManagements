package Vibol.SEN.meetingManagements.repository;

import Vibol.SEN.meetingManagements.model.ActionItem;
import Vibol.SEN.meetingManagements.model.enums.ActionItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActionItemRepository extends JpaRepository<ActionItem, Long> {
    List<ActionItem> findByMeeting_MeetingId(Long meetingId);
    List<ActionItem> findByAssignee_UserId(Long userId);
    List<ActionItem> findByAssignee_UserIdAndStatus(Long userId, ActionItemStatus status);
}
