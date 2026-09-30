package Vibol.SEN.meetingManagements.service;

import Vibol.SEN.meetingManagements.dto.MaterialRequest;
import Vibol.SEN.meetingManagements.dto.MaterialResponse;
import Vibol.SEN.meetingManagements.exception.BadRequestException;
import Vibol.SEN.meetingManagements.exception.ResourceNotFoundException;
import Vibol.SEN.meetingManagements.model.Material;
import Vibol.SEN.meetingManagements.model.enums.MaterialType;
import Vibol.SEN.meetingManagements.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MaterialService {

    private final MaterialRepository materialRepository;

    @Transactional(readOnly = true)
    public List<MaterialResponse> getAllMaterials() {
        return materialRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MaterialResponse getMaterialById(Long id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with ID: " + id));
        return mapToResponse(material);
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> getMaterialsByType(MaterialType type) {
        return materialRepository.findByType(type).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public MaterialResponse createMaterial(MaterialRequest request) {
        Material material = Material.builder()
                .name(request.getName())
                .type(request.getType())
                .quantityAvailable(request.getQuantityAvailable())
                .build();
        Material saved = materialRepository.save(material);
        return mapToResponse(saved);
    }

    public MaterialResponse updateMaterial(Long id, MaterialRequest request) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with ID: " + id));

        material.setName(request.getName());
        material.setType(request.getType());
        material.setQuantityAvailable(request.getQuantityAvailable());

        Material updated = materialRepository.save(material);
        return mapToResponse(updated);
    }

    public void deleteMaterial(Long id) {
        if (!materialRepository.existsById(id)) {
            throw new ResourceNotFoundException("Material not found with ID: " + id);
        }
        materialRepository.deleteById(id);
    }

    public void reserveMaterialStock(Long materialId, int quantity) {
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with ID: " + materialId));

        if (material.getQuantityAvailable() < quantity) {
            throw new BadRequestException("Insufficient stock for material: " + material.getName() +
                    ". Available: " + material.getQuantityAvailable() + ", Requested: " + quantity);
        }

        material.setQuantityAvailable(material.getQuantityAvailable() - quantity);
        materialRepository.save(material);
    }

    public void releaseMaterialStock(Long materialId, int quantity) {
        Material material = materialRepository.findById(materialId).orElse(null);
        if (material != null) {
            material.setQuantityAvailable(material.getQuantityAvailable() + quantity);
            materialRepository.save(material);
        }
    }

    public MaterialResponse mapToResponse(Material material) {
        return MaterialResponse.builder()
                .materialId(material.getMaterialId())
                .name(material.getName())
                .type(material.getType())
                .quantityAvailable(material.getQuantityAvailable())
                .build();
    }
}
