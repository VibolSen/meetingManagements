package Vibol.SEN.meetingManagements.service;

import Vibol.SEN.meetingManagements.dto.RoomAvailabilityDTO;
import Vibol.SEN.meetingManagements.dto.RoomRequest;
import Vibol.SEN.meetingManagements.dto.RoomResponse;
import Vibol.SEN.meetingManagements.exception.BadRequestException;
import Vibol.SEN.meetingManagements.exception.ResourceNotFoundException;
import Vibol.SEN.meetingManagements.model.Meeting;
import Vibol.SEN.meetingManagements.model.Room;
import Vibol.SEN.meetingManagements.model.enums.RoomStatus;
import Vibol.SEN.meetingManagements.repository.MeetingRepository;
import Vibol.SEN.meetingManagements.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RoomService {

    private final RoomRepository roomRepository;
    private final MeetingRepository meetingRepository;

    @Transactional(readOnly = true)
    public List<RoomResponse> getAllRooms() {
        return roomRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoomById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + id));
        return mapToResponse(room);
    }

    public RoomResponse createRoom(RoomRequest request) {
        Room room = Room.builder()
                .name(request.getName())
                .location(request.getLocation())
                .capacity(request.getCapacity())
                .status(request.getStatus())
                .build();
        Room saved = roomRepository.save(room);
        return mapToResponse(saved);
    }

    public RoomResponse updateRoom(Long id, RoomRequest request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + id));

        room.setName(request.getName());
        room.setLocation(request.getLocation());
        room.setCapacity(request.getCapacity());
        room.setStatus(request.getStatus());

        Room updated = roomRepository.save(room);
        return mapToResponse(updated);
    }

    public void deleteRoom(Long id) {
        if (!roomRepository.existsById(id)) {
            throw new ResourceNotFoundException("Room not found with ID: " + id);
        }
        roomRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public RoomAvailabilityDTO checkRoomAvailability(Long roomId, LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end) || start.isEqual(end)) {
            throw new BadRequestException("Start time must be before end time");
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));

        if (room.getStatus() != RoomStatus.ACTIVE) {
            return RoomAvailabilityDTO.builder()
                    .roomId(room.getRoomId())
                    .name(room.getName())
                    .location(room.getLocation())
                    .capacity(room.getCapacity())
                    .status(room.getStatus())
                    .available(false)
                    .conflictReason("Room is " + room.getStatus())
                    .build();
        }

        List<Meeting> conflicts = meetingRepository.findOverlappingMeetings(roomId, start, end);
        boolean isAvailable = conflicts.isEmpty();
        String conflictReason = isAvailable ? null : "Room is occupied by meeting: '" + conflicts.get(0).getTitle() + "'";
        LocalDateTime nextAvailable = isAvailable ? null : conflicts.get(conflicts.size() - 1).getEndTime();

        return RoomAvailabilityDTO.builder()
                .roomId(room.getRoomId())
                .name(room.getName())
                .location(room.getLocation())
                .capacity(room.getCapacity())
                .status(room.getStatus())
                .available(isAvailable)
                .conflictReason(conflictReason)
                .nextAvailableTime(nextAvailable)
                .build();
    }

    @Transactional(readOnly = true)
    public List<RoomAvailabilityDTO> getAvailableRoomsForSlot(LocalDateTime start, LocalDateTime end, Integer minCapacity) {
        if (start.isAfter(end) || start.isEqual(end)) {
            throw new BadRequestException("Start time must be before end time");
        }

        List<Room> activeRooms = minCapacity != null && minCapacity > 0
                ? roomRepository.findByStatusAndCapacityGreaterThanEqual(RoomStatus.ACTIVE, minCapacity)
                : roomRepository.findByStatus(RoomStatus.ACTIVE);

        return activeRooms.stream()
                .map(room -> {
                    boolean hasConflict = meetingRepository.existsOverlappingMeeting(room.getRoomId(), start, end);
                    return RoomAvailabilityDTO.builder()
                            .roomId(room.getRoomId())
                            .name(room.getName())
                            .location(room.getLocation())
                            .capacity(room.getCapacity())
                            .status(room.getStatus())
                            .available(!hasConflict)
                            .conflictReason(hasConflict ? "Already booked for selected time" : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public RoomResponse mapToResponse(Room room) {
        return RoomResponse.builder()
                .roomId(room.getRoomId())
                .name(room.getName())
                .location(room.getLocation())
                .capacity(room.getCapacity())
                .status(room.getStatus())
                .build();
    }
}
