package Vibol.SEN.meetingManagements.repository;

import Vibol.SEN.meetingManagements.model.Meeting;
import Vibol.SEN.meetingManagements.model.enums.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    List<Meeting> findByOrganizer_UserId(Long organizerId);

    List<Meeting> findByStatus(MeetingStatus status);

    List<Meeting> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    List<Meeting> findByRoom_RoomIdAndStartTimeBetween(Long roomId, LocalDateTime start, LocalDateTime end);

    /**
     * Finds overlapping meetings for a given room within [startTime, endTime) excluding CANCELLED meetings.
     */
    @Query("SELECT m FROM Meeting m " +
           "WHERE m.room.roomId = :roomId " +
           "AND m.status != 'CANCELLED' " +
           "AND m.startTime < :endTime " +
           "AND m.endTime > :startTime")
    List<Meeting> findOverlappingMeetings(
            @Param("roomId") Long roomId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Checks if any active meeting in the given room overlaps with [startTime, endTime).
     */
    @Query("SELECT COUNT(m) > 0 FROM Meeting m " +
           "WHERE m.room.roomId = :roomId " +
           "AND m.status != 'CANCELLED' " +
           "AND m.startTime < :endTime " +
           "AND m.endTime > :startTime")
    boolean existsOverlappingMeeting(
            @Param("roomId") Long roomId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Checks if any active meeting in the given room overlaps with [startTime, endTime), excluding a specific meeting ID (used for meeting updates).
     */
    @Query("SELECT COUNT(m) > 0 FROM Meeting m " +
           "WHERE m.room.roomId = :roomId " +
           "AND m.meetingId != :excludeMeetingId " +
           "AND m.status != 'CANCELLED' " +
           "AND m.startTime < :endTime " +
           "AND m.endTime > :startTime")
    boolean existsOverlappingMeetingExcluding(
            @Param("roomId") Long roomId,
            @Param("excludeMeetingId") Long excludeMeetingId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
