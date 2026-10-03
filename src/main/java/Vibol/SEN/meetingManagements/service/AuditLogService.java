package Vibol.SEN.meetingManagements.service;

import Vibol.SEN.meetingManagements.dto.AuditLogResponse;
import Vibol.SEN.meetingManagements.dto.AuditLogSummaryResponse;
import Vibol.SEN.meetingManagements.model.AuditLog;
import Vibol.SEN.meetingManagements.model.User;
import Vibol.SEN.meetingManagements.model.enums.AuditActionType;
import Vibol.SEN.meetingManagements.model.enums.AuditEntityType;
import Vibol.SEN.meetingManagements.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLog recordLog(
            Long actorId,
            String actorName,
            String actorEmail,
            AuditActionType actionType,
            AuditEntityType entityType,
            Long entityId,
            String entityName,
            String details,
            String ipAddress
    ) {
        AuditLog auditLog = AuditLog.builder()
                .actorId(actorId)
                .actorName(actorName != null ? actorName : "System Engine")
                .actorEmail(actorEmail != null ? actorEmail : "system@meeting.internal")
                .actionType(actionType)
                .entityType(entityType)
                .entityId(entityId)
                .entityName(entityName)
                .details(details)
                .ipAddress(ipAddress != null ? ipAddress : "127.0.0.1")
                .build();

        AuditLog saved = auditLogRepository.save(auditLog);
        log.info("AUDIT [{}] {} on {} id={} by {}", actionType, entityType, entityName, entityId, actorName);
        return saved;
    }

    public AuditLog recordUserAction(
            User actor,
            AuditActionType actionType,
            AuditEntityType entityType,
            Long entityId,
            String entityName,
            String details
    ) {
        if (actor != null) {
            return recordLog(
                    actor.getUserId(),
                    actor.getName(),
                    actor.getEmail(),
                    actionType,
                    entityType,
                    entityId,
                    entityName,
                    details,
                    "127.0.0.1"
            );
        } else {
            return recordSystemAction(actionType, entityType, entityId, entityName, details);
        }
    }

    public AuditLog recordSystemAction(
            AuditActionType actionType,
            AuditEntityType entityType,
            Long entityId,
            String entityName,
            String details
    ) {
        return recordLog(
                null,
                "System Automation",
                "system@meeting.internal",
                actionType,
                entityType,
                entityId,
                entityName,
                details,
                "127.0.0.1"
        );
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getLogs(
            AuditActionType actionType,
            AuditEntityType entityType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String keyword,
            Pageable pageable
    ) {
        return auditLogRepository
                .searchLogs(actionType, entityType, startDate, endDate, keyword, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public AuditLogResponse getLogById(Long logId) {
        AuditLog log = auditLogRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("Audit log not found with ID: " + logId));
        return mapToResponse(log);
    }

    @Transactional(readOnly = true)
    public AuditLogSummaryResponse getSummary() {
        long total = auditLogRepository.count();
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        long today = auditLogRepository.countByCreatedAtAfter(startOfToday);
        long approvals = auditLogRepository.countByActionType(AuditActionType.APPROVE);
        long security = auditLogRepository.countByActionType(AuditActionType.ROLE_CHANGE)
                + auditLogRepository.countByActionType(AuditActionType.LOGIN);

        Map<String, Long> actionDist = new HashMap<>();
        for (AuditActionType type : AuditActionType.values()) {
            actionDist.put(type.name(), auditLogRepository.countByActionType(type));
        }

        return AuditLogSummaryResponse.builder()
                .totalLogs(total)
                .logsToday(today)
                .approvalActions(approvals)
                .securityActions(security)
                .actionDistribution(actionDist)
                .build();
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(
            AuditActionType actionType,
            AuditEntityType entityType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String keyword
    ) {
        List<AuditLog> logs = auditLogRepository.exportLogs(actionType, entityType, startDate, endDate, keyword);
        StringBuilder sb = new StringBuilder();
        sb.append("Log ID,Timestamp,Actor Name,Actor Email,Action Type,Entity Type,Entity ID,Entity Name,IP Address,Details\n");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (AuditLog l : logs) {
            sb.append(l.getLogId()).append(",");
            sb.append(l.getCreatedAt() != null ? l.getCreatedAt().format(dtf) : "").append(",");
            sb.append(escapeCsv(l.getActorName())).append(",");
            sb.append(escapeCsv(l.getActorEmail())).append(",");
            sb.append(l.getActionType()).append(",");
            sb.append(l.getEntityType()).append(",");
            sb.append(l.getEntityId() != null ? l.getEntityId() : "").append(",");
            sb.append(escapeCsv(l.getEntityName())).append(",");
            sb.append(escapeCsv(l.getIpAddress())).append(",");
            sb.append(escapeCsv(l.getDetails())).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String val) {
        if (val == null) return "\"\"";
        return "\"" + val.replace("\"", "\"\"").replace("\n", " ").replace("\r", "") + "\"";
    }

    public AuditLogResponse mapToResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .logId(log.getLogId())
                .actorId(log.getActorId())
                .actorName(log.getActorName())
                .actorEmail(log.getActorEmail())
                .actionType(log.getActionType())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .entityName(log.getEntityName())
                .details(log.getDetails())
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
