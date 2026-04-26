package be.ephec.pdw.projetwebbackendpadel.controller;

import be.ephec.pdw.projetwebbackendpadel.dto.siteDto.SiteRequest;
import be.ephec.pdw.projetwebbackendpadel.dto.siteDto.SiteResponse;
import be.ephec.pdw.projetwebbackendpadel.service.SiteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/sites")
@RequiredArgsConstructor
public class SiteController {
    private final SiteService siteService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SiteResponse>> findAll(
            @RequestParam(required = false) String search) {
        List<SiteResponse> result = (search != null && !search.isBlank())
                ? siteService.search(search)
                : siteService.findAll();

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/v1/sites/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SiteResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(siteService.findById(id));
    }

    /**
     * GET /api/v1/sites/ouverts?date=2025-06-15
     * Sites ouverts à une date donnée (hors jours de fermeture).
     */
    @GetMapping("/ouverts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SiteResponse>> findSitesOuverts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(siteService.findSitesOuverts(date));
    }

    /**
     * POST /api/v1/sites
     * Création d'un site — réservé ADMIN.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SiteResponse> create(@Valid @RequestBody SiteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(siteService.create(request));
    }

    /**
     * PUT /api/v1/sites/{id}
     * Mise à jour complète — réservé ADMIN.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SiteResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SiteRequest request) {
        return ResponseEntity.ok(siteService.update(id, request));
    }

    /**
     * DELETE /api/v1/sites/{id}
     * Suppression — réservé ADMIN.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        siteService.delete(id);
        return ResponseEntity.noContent().build();
    }


}
