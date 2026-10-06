package Vibol.SEN.meetingManagements.service;

import Vibol.SEN.meetingManagements.dto.AnalyticsSummaryResponse;
import Vibol.SEN.meetingManagements.dto.DepartmentUsageDTO;
import Vibol.SEN.meetingManagements.dto.HeatmapCellDTO;
import Vibol.SEN.meetingManagements.dto.RoomUtilizationDTO;
import Vibol.SEN.meetingManagements.model.Meeting;
import Vibol.SEN.meetingManagements.model.Room;
import Vibol.SEN.meetingManagements.model.enums.MeetingStatus;
import Vibol.SEN.meetingManagements.repository.MeetingRepository;
import Vibol.SEN.meetingManagements.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final MeetingRepository meetingRepository;
    private final RoomRepository roomRepository;

    public AnalyticsSummaryResponse getAnalyticsSummary(LocalDateTime start, LocalDateTime end) {
        if (start == null) {
            start = LocalDate.now().minusDays(30).atStartOfDay();
        }
        if (end == null) {
            end = LocalDateTime.now();
        }

        List<Meeting> meetings = meetingRepository.findByStartTimeBetween(start, end);
        List<Room> rooms = roomRepository.findAll();

        long totalBookings = meetings.size();
        long completedBookings = meetings.stream().filter(m -> m.getStatus() == MeetingStatus.COMPLETED).count();
        long cancelledBookings = meetings.stream().filter(m -> m.getStatus() == MeetingStatus.CANCELLED).count();

        // Auto released no shows
        long autoReleasedNoShows = meetings.stream()
                .filter(m -> m.getStatus() == MeetingStatus.CANCELLED)
                .filter(m -> (m.getPurpose() != null && m.getPurpose().contains("AUTO_RELEASE_NO_SHOW")))
                .count();

        // Total meeting hours for CONFIRMED / COMPLETED
        double totalMeetingHours = meetings.stream()
                .filter(m -> m.getStatus() == MeetingStatus.CONFIRMED || m.getStatus() == MeetingStatus.COMPLETED)
                .mapToDouble(m -> Math.max(0, Duration.between(m.getStartTime(), m.getEndTime()).toMinutes()) / 60.0)
                .sum();

        long daysInRange = Math.max(1, ChronoUnit.DAYS.between(start, end));
        double operatingHoursPerDay = 10.0; // 08:00 - 18:00
        double totalFacilityCapacityHours = rooms.size() * daysInRange * operatingHoursPerDay;

        double overallFacilityUtilizationPct = totalFacilityCapacityHours > 0
                ? Math.min(100.0, Math.round((totalMeetingHours / totalFacilityCapacityHours) * 1000.0) / 10.0)
                : 0.0;

        double cancellationRatePct = totalBookings > 0
                ? Math.round(((double) cancelledBookings / totalBookings) * 1000.0) / 10.0
                : 0.0;

        double noShowRatePct = totalBookings > 0
                ? Math.round(((double) autoReleasedNoShows / totalBookings) * 1000.0) / 10.0
                : 0.0;

        // Room Utilization Breakdown
        List<RoomUtilizationDTO> roomUtilizations = new ArrayList<>();
        for (Room room : rooms) {
            List<Meeting> roomMeetings = meetings.stream()
                    .filter(m -> m.getRoom() != null && m.getRoom().getRoomId().equals(room.getRoomId()))
                    .filter(m -> m.getStatus() == MeetingStatus.CONFIRMED || m.getStatus() == MeetingStatus.COMPLETED)
                    .collect(Collectors.toList());

            double roomHours = roomMeetings.stream()
                    .mapToDouble(m -> Math.max(0, Duration.between(m.getStartTime(), m.getEndTime()).toMinutes()) / 60.0)
                    .sum();

            double roomCapacityHours = daysInRange * operatingHoursPerDay;
            double roomUtilizationPct = roomCapacityHours > 0
                    ? Math.min(100.0, Math.round((roomHours / roomCapacityHours) * 1000.0) / 10.0)
                    : 0.0;

            roomUtilizations.add(RoomUtilizationDTO.builder()
                    .roomId(room.getRoomId())
                    .roomName(room.getName())
                    .capacity(room.getCapacity())
                    .totalMeetings((long) roomMeetings.size())
                    .totalHours(Math.round(roomHours * 10.0) / 10.0)
                    .utilizationPct(roomUtilizationPct)
                    .build());
        }

        // Sort rooms by utilization desc
        roomUtilizations.sort((a, b) -> Double.compare(b.getUtilizationPct(), a.getUtilizationPct()));

        // Department Usage Breakdown
        Map<String, List<Meeting>> deptMap = new HashMap<>();
        for (Meeting m : meetings) {
            if (m.getStatus() == MeetingStatus.CONFIRMED || m.getStatus() == MeetingStatus.COMPLETED) {
                String deptName = (m.getOrganizer() != null && m.getOrganizer().getDepartment() != null)
                        ? m.getOrganizer().getDepartment().getName()
                        : "Operations & General";
                deptMap.computeIfAbsent(deptName, k -> new ArrayList<>()).add(m);
            }
        }

        List<DepartmentUsageDTO> departmentUsages = new ArrayList<>();
        for (Map.Entry<String, List<Meeting>> entry : deptMap.entrySet()) {
            double deptHours = entry.getValue().stream()
                    .mapToDouble(m -> Math.max(0, Duration.between(m.getStartTime(), m.getEndTime()).toMinutes()) / 60.0)
                    .sum();
            double deptPct = totalMeetingHours > 0
                    ? Math.round((deptHours / totalMeetingHours) * 1000.0) / 10.0
                    : 0.0;

            departmentUsages.add(DepartmentUsageDTO.builder()
                    .departmentName(entry.getKey())
                    .totalMeetings((long) entry.getValue().size())
                    .totalHours(Math.round(deptHours * 10.0) / 10.0)
                    .percentage(deptPct)
                    .build());
        }
        departmentUsages.sort((a, b) -> Double.compare(b.getTotalHours(), a.getTotalHours()));

        // Peak Hours Heatmap (Days Mon-Fri: 1 to 5, Hours 8 to 18)
        String[] dayNames = {"", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        Map<String, Integer> cellCounts = new HashMap<>();
        int maxHourlyCount = 0;

        for (Meeting m : meetings) {
            if (m.getStatus() != MeetingStatus.CANCELLED) {
                int dayOfWeek = m.getStartTime().getDayOfWeek().getValue();
                int startHour = m.getStartTime().getHour();
                int endHour = m.getEndTime().getHour();

                for (int h = Math.max(8, startHour); h <= Math.min(18, endHour); h++) {
                    String key = dayOfWeek + "_" + h;
                    int count = cellCounts.getOrDefault(key, 0) + 1;
                    cellCounts.put(key, count);
                    if (count > maxHourlyCount) maxHourlyCount = count;
                }
            }
        }

        List<HeatmapCellDTO> heatmap = new ArrayList<>();
        for (int d = 1; d <= 5; d++) {
            for (int h = 8; h <= 18; h++) {
                String key = d + "_" + h;
                int count = cellCounts.getOrDefault(key, 0);
                double intensity = maxHourlyCount > 0 ? (double) count / maxHourlyCount : 0.0;
                heatmap.add(HeatmapCellDTO.builder()
                        .dayOfWeek(d)
                        .dayName(dayNames[d])
                        .hour(h)
                        .meetingCount(count)
                        .intensity(Math.round(intensity * 100.0) / 100.0)
                        .build());
            }
        }

        return AnalyticsSummaryResponse.builder()
                .totalBookings(totalBookings)
                .completedBookings(completedBookings)
                .cancelledBookings(cancelledBookings)
                .autoReleasedNoShows(autoReleasedNoShows)
                .totalMeetingHours(Math.round(totalMeetingHours * 10.0) / 10.0)
                .overallFacilityUtilizationPct(overallFacilityUtilizationPct)
                .cancellationRatePct(cancellationRatePct)
                .noShowRatePct(noShowRatePct)
                .roomUtilizations(roomUtilizations)
                .departmentUsages(departmentUsages)
                .heatmap(heatmap)
                .build();
    }
}
