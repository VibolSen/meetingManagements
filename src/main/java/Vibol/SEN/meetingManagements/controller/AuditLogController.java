package Vibol.SEN.meetingManagements.controller;

import Vibol.SEN.meetingManagements.dto.AuditLogResponse;
import Vibol.SEN.meetingManagements.dto.AuditLogSummaryResponse;
import Vibol.SEN.meetingManagements.model.enums.AuditActionType;
import Vibol.SEN.meetingManagements.model.enums.AuditEntityType;
import Vibol.SEN.meetingManagements.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> getLogs(
            @RequestParam(required = false) AuditActionType actionType,
            @RequestParam(required = false) AuditEntityType entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(auditLogService.getLogs(actionType, entityType, startDate, endDate, keyword, pageable));
    }

    @GetMapping("/summary")
    public ResponseEntity<AuditLogSummaryResponse> getSummary() {
        return ResponseEntity.ok(auditLogService.getSummary());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditLogResponse> getLogById(@PathVariable Long id) {
        return ResponseEntity.ok(auditLogService.getLogById(id));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) AuditActionType actionType,
            @RequestParam(required = false) AuditEntityType entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String keyword
    ) {
        byte[] csvData = auditLogService.exportCsv(actionType, entityType, startDate, endDate, keyword);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "mms-audit-logs-" + System.currentTimeMillis() + ".csv");

        return ResponseEntity.ok().headers(headers).body(csvData);
    }
}
