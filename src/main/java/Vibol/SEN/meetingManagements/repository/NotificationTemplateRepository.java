package Vibol.SEN.meetingManagements.repository;

import Vibol.SEN.meetingManagements.model.NotificationTemplate;
import Vibol.SEN.meetingManagements.model.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    Optional<NotificationTemplate> findByType(NotificationType type);
    boolean existsByType(NotificationType type);
}
