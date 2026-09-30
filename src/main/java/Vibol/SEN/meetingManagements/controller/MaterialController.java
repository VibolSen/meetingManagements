package Vibol.SEN.meetingManagements.controller;

import Vibol.SEN.meetingManagements.dto.MaterialRequest;
import Vibol.SEN.meetingManagements.dto.MaterialResponse;
import Vibol.SEN.meetingManagements.model.enums.MaterialType;
import Vibol.SEN.meetingManagements.service.MaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping
    public ResponseEntity<List<MaterialResponse>> getAllMaterials() {
        return ResponseEntity.ok(materialService.getAllMaterials());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaterialResponse> getMaterialById(@PathVariable Long id) {
        return ResponseEntity.ok(materialService.getMaterialById(id));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<MaterialResponse>> getMaterialsByType(@PathVariable MaterialType type) {
        return ResponseEntity.ok(materialService.getMaterialsByType(type));
    }

    @PostMapping
    public ResponseEntity<MaterialResponse> createMaterial(@Valid @RequestBody MaterialRequest request) {
        return new ResponseEntity<>(materialService.createMaterial(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaterialResponse> updateMaterial(@PathVariable Long id, @Valid @RequestBody MaterialRequest request) {
        return ResponseEntity.ok(materialService.updateMaterial(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMaterial(@PathVariable Long id) {
        materialService.deleteMaterial(id);
        return ResponseEntity.noContent().build();
    }
}
