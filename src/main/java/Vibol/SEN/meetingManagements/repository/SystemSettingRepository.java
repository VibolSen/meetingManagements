package Vibol.SEN.meetingManagements.repository;

import Vibol.SEN.meetingManagements.model.SystemSetting;
import Vibol.SEN.meetingManagements.model.enums.SettingCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {
    List<SystemSetting> findByCategory(SettingCategory category);
    List<SystemSetting> findByIsPublicTrue();
}
