package be.ephec.pdw.projetwebbackendpadel.controller;

import be.ephec.pdw.projetwebbackendpadel.dto.membreDto.MembreRequest;
import be.ephec.pdw.projetwebbackendpadel.dto.membreDto.MembreResponse;
import be.ephec.pdw.projetwebbackendpadel.enums.TypeMembre;
import be.ephec.pdw.projetwebbackendpadel.service.MembreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/membres")
@RequiredArgsConstructor
public class MembreController {

    private final MembreService membreService;

    /**
     * GET /api/v1/membres
     * Paramètres optionnels : ?search= ou ?type= ou ?siteId=
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MembreResponse>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TypeMembre type,
            @RequestParam(required = false) Long siteId) {

        List<MembreResponse> result;

        if (search != null && !search.isBlank()) {
            result = membreService.search(search);
        } else if (type != null) {
            result = membreService.findByType(type);
        } else if (siteId != null) {
            result = membreService.findBySite(siteId);
        }  else {
            result = membreService.findAll();
        }

        return ResponseEntity.ok(result);
    }

    /** GET /api/v1/membres/{id} */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @membreSecurityService.isSelf(#id)")
    public ResponseEntity<MembreResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(membreService.findById(id));
    }

    /** GET /api/v1/membres/soldes-impayes — tableau de bord admin */
    @GetMapping("/soldes-impayes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MembreResponse>> findAvecSoldeImpaye() {
        return ResponseEntity.ok(membreService.findAvecSoldeImpaye());
    }

    /** POST /api/v1/membres */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MembreResponse> create(
            @Valid @RequestBody MembreRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(membreService.create(request));
    }

    /** PUT /api/v1/membres/{id} */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MembreResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody MembreRequest request) {
        return ResponseEntity.ok(membreService.update(id, request));
    }

    /** DELETE /api/v1/membres/{id} */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        membreService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
