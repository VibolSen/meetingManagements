package Vibol.SEN.meetingManagements.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringPreviewResponse {

    private int totalGenerated;
    private int clearCount;
    private int conflictCount;

    @Builder.Default
    private List<RecurringSlotDTO> slots = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecurringSlotDTO {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private boolean isAvailable;
        private String conflictReason;
    }
}
