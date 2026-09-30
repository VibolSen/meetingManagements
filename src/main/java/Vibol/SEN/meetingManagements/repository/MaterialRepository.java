package Vibol.SEN.meetingManagements.repository;

import Vibol.SEN.meetingManagements.model.Material;
import Vibol.SEN.meetingManagements.model.enums.MaterialType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    Optional<Material> findByName(String name);
    List<Material> findByType(MaterialType type);
    List<Material> findByQuantityAvailableGreaterThanEqual(Integer minQuantity);
}
