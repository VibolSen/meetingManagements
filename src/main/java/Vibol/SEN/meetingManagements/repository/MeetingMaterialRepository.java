package Vibol.SEN.meetingManagements.repository;

import Vibol.SEN.meetingManagements.model.MeetingMaterial;
import Vibol.SEN.meetingManagements.model.MeetingMaterialId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingMaterialRepository extends JpaRepository<MeetingMaterial, MeetingMaterialId> {
    List<MeetingMaterial> findById_MeetingId(Long meetingId);
    List<MeetingMaterial> findById_MaterialId(Long materialId);
    void deleteById_MeetingId(Long meetingId);
}
