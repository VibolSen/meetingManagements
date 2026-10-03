package Vibol.SEN.meetingManagements.repository;

import Vibol.SEN.meetingManagements.model.AuditLog;
import Vibol.SEN.meetingManagements.model.enums.AuditActionType;
import Vibol.SEN.meetingManagements.model.enums.AuditEntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:actionType IS NULL OR a.actionType = :actionType) AND " +
           "(:entityType IS NULL OR a.entityType = :entityType) AND " +
           "(:startDate IS NULL OR a.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR a.createdAt <= :endDate) AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(a.actorName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(a.actorEmail) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(a.entityName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(a.details) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> searchLogs(
            @Param("actionType") AuditActionType actionType,
            @Param("entityType") AuditEntityType entityType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:actionType IS NULL OR a.actionType = :actionType) AND " +
           "(:entityType IS NULL OR a.entityType = :entityType) AND " +
           "(:startDate IS NULL OR a.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR a.createdAt <= :endDate) AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(a.actorName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(a.actorEmail) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(a.entityName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(a.details) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY a.createdAt DESC")
    List<AuditLog> exportLogs(
            @Param("actionType") AuditActionType actionType,
            @Param("entityType") AuditEntityType entityType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("keyword") String keyword
    );

    long countByCreatedAtAfter(LocalDateTime timestamp);

    long countByActionType(AuditActionType actionType);
}
