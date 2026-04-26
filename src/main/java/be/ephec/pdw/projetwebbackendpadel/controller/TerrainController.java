package be.ephec.pdw.projetwebbackendpadel.controller;

import be.ephec.pdw.projetwebbackendpadel.dto.terrainDto.TerrainRequest;
import be.ephec.pdw.projetwebbackendpadel.dto.terrainDto.TerrainResponse;
import be.ephec.pdw.projetwebbackendpadel.service.TerrainService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/v1/terrains")
@RequiredArgsConstructor
public class TerrainController {

    private final TerrainService terrainService;

    /**
     * GET /api/v1/terrains?siteId=1
     * Tous les terrains d'un site.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TerrainResponse>> findBySite(
            @RequestParam Long siteId) {
        return ResponseEntity.ok(terrainService.findBySite(siteId));
    }

    /**
     * GET /api/v1/terrains/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TerrainResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(terrainService.findById(id));
    }

    /**
     * GET /api/v1/terrains/disponibles?siteId=1&dateHeure=2025-06-15T10:00:00
     * Terrains libres à un créneau donné — utilisé lors de la création d'un match.
     */
    @GetMapping("/disponibles")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TerrainResponse>> findDisponibles(
            @RequestParam Long siteId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime dateHeure) {
        return ResponseEntity.ok(terrainService.findDisponibles(siteId, dateHeure));
    }

    /**
     * POST /api/v1/terrains
     * Création — réservé ADMIN.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TerrainResponse> create(
            @Valid @RequestBody TerrainRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(terrainService.create(request));
    }

    /**
     * PUT /api/v1/terrains/{id}
     * Mise à jour — réservé ADMIN.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TerrainResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TerrainRequest request) {
        return ResponseEntity.ok(terrainService.update(id, request));
    }

    /**
     * DELETE /api/v1/terrains/{id}
     * Suppression — réservé ADMIN.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        terrainService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
