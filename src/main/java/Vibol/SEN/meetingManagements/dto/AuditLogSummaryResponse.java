package Vibol.SEN.meetingManagements.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogSummaryResponse {
    private long totalLogs;
    private long logsToday;
    private long approvalActions;
    private long securityActions;
    private Map<String, Long> actionDistribution;
    private Map<String, Long> entityDistribution;
}
