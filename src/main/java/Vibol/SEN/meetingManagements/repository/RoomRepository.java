package Vibol.SEN.meetingManagements.repository;

import Vibol.SEN.meetingManagements.model.Room;
import Vibol.SEN.meetingManagements.model.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByStatus(RoomStatus status);
    List<Room> findByCapacityGreaterThanEqual(Integer capacity);
    List<Room> findByStatusAndCapacityGreaterThanEqual(RoomStatus status, Integer capacity);
}
