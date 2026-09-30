package Vibol.SEN.meetingManagements.service;

import Vibol.SEN.meetingManagements.dto.DashboardSummaryResponse;
import Vibol.SEN.meetingManagements.dto.MeetingResponse;
import Vibol.SEN.meetingManagements.model.enums.MeetingStatus;
import Vibol.SEN.meetingManagements.model.enums.RoomStatus;
import Vibol.SEN.meetingManagements.model.enums.StaffAvailability;
import Vibol.SEN.meetingManagements.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final MeetingRepository meetingRepository;
    private final RoomRepository roomRepository;
    private final StaffRepository staffRepository;
    private final MaterialRepository materialRepository;
    private final MeetingService meetingService;

    public DashboardSummaryResponse getDashboardSummary() {
        long totalMeetings = meetingRepository.count();
        long pendingMeetings = meetingRepository.findByStatus(MeetingStatus.PENDING).size();
        long confirmedMeetings = meetingRepository.findByStatus(MeetingStatus.CONFIRMED).size();
        long completedMeetings = meetingRepository.findByStatus(MeetingStatus.COMPLETED).size();
        long cancelledMeetings = meetingRepository.findByStatus(MeetingStatus.CANCELLED).size();

        long totalRooms = roomRepository.count();
        long activeRooms = roomRepository.findByStatus(RoomStatus.ACTIVE).size();

        long totalStaff = staffRepository.count();
        long availableStaff = staffRepository.findByAvailabilityStatus(StaffAvailability.AVAILABLE).size();

        long totalMaterials = materialRepository.count();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneWeekLater = now.plusDays(7);
        List<MeetingResponse> upcoming = meetingRepository.findByStartTimeBetween(now, oneWeekLater).stream()
                .filter(m -> m.getStatus() == MeetingStatus.CONFIRMED || m.getStatus() == MeetingStatus.PENDING)
                .limit(10)
                .map(meetingService::mapToResponse)
                .collect(Collectors.toList());

        return DashboardSummaryResponse.builder()
                .totalMeetings(totalMeetings)
                .pendingMeetings(pendingMeetings)
                .confirmedMeetings(confirmedMeetings)
                .completedMeetings(completedMeetings)
                .cancelledMeetings(cancelledMeetings)
                .totalRooms(totalRooms)
                .activeRooms(activeRooms)
                .totalStaff(totalStaff)
                .availableStaff(availableStaff)
                .totalMaterials(totalMaterials)
                .upcomingMeetings(upcoming)
                .build();
    }
}
