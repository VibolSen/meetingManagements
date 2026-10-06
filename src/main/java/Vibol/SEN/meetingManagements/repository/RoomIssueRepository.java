package Vibol.SEN.meetingManagements.repository;

import Vibol.SEN.meetingManagements.model.RoomIssue;
import Vibol.SEN.meetingManagements.model.enums.RoomIssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomIssueRepository extends JpaRepository<RoomIssue, Long> {

    List<RoomIssue> findByRoom_RoomIdOrderByCreatedAtDesc(Long roomId);

    List<RoomIssue> findAllByOrderByCreatedAtDesc();

    List<RoomIssue> findByStatus(RoomIssueStatus status);

    long countByRoom_RoomIdAndStatusIn(Long roomId, List<RoomIssueStatus> statuses);
}
