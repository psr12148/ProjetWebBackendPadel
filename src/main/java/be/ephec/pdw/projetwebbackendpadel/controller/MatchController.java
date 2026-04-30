package be.ephec.pdw.projetwebbackendpadel.controller;

import be.ephec.pdw.projetwebbackendpadel.dto.matchDto.MatchRequest;
import be.ephec.pdw.projetwebbackendpadel.dto.matchDto.MatchResponse;
import be.ephec.pdw.projetwebbackendpadel.dto.participationDto.ParticipationRequest;
import be.ephec.pdw.projetwebbackendpadel.dto.participationDto.ParticipationResponse;
import be.ephec.pdw.projetwebbackendpadel.service.MatchService;
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
@RequestMapping("/v1/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    /**
     * GET /api/v1/matchs/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MatchResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(matchService.findById(id));
    }

    /**
     * GET /api/v1/matchs/site/{siteId}?date=2025-06-15
     * Matchs d'un site pour une date donnée (vue calendrier).
     */
    @GetMapping("/site/{siteId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MatchResponse>> findBySiteAndDate(
            @PathVariable Long siteId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(matchService.findBySiteAndDate(siteId, date));
    }

    /**
     * GET /api/v1/matchs/publics
     * Matchs publics avec places disponibles.
     */
    @GetMapping("/publics")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MatchResponse>> findPublicsDisponibles() {
        return ResponseEntity.ok(matchService.findMatchsPublicsDisponibles());
    }

    /**
     * GET /api/v1/matchs/membre/{membreId}
     * Historique des matchs d'un membre.
     */
    @GetMapping("/membre/{membreId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MatchResponse>> findByMembre(
            @PathVariable Long membreId) {
        return ResponseEntity.ok(matchService.findByMembre(membreId));
    }

    /**
     * POST /api/v1/matchs/organisateur/{organisateurId}
     * Création d'un match par un organisateur.
     */
    @PostMapping("/organisateur/{organisateurId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MatchResponse> creerMatch(
            @PathVariable Long organisateurId,
            @Valid @RequestBody MatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(matchService.creerMatch(organisateurId, request));
    }

    /**
     * POST /api/v1/matchs/{id}/rejoindre/{membreId}
     * Un membre rejoint un match public.
     */
    @PostMapping("/{id}/rejoindre/{membreId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ParticipationResponse> rejoindreMatchPublic(
            @PathVariable Long id,
            @PathVariable Long membreId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(matchService.rejoindreMatchPublic(id, membreId));
    }

    /**
     * POST /api/v1/matchs/{id}/joueurs/organisateur/{organisateurId}
     * L'organisateur ajoute un joueur à son match privé.
     */
    @PostMapping("/{id}/joueurs/organisateur/{organisateurId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ParticipationResponse> ajouterJoueurPrive(
            @PathVariable Long id,
            @PathVariable Long organisateurId,
            @Valid @RequestBody ParticipationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(matchService.ajouterJoueurPrive(
                        id, organisateurId, request.getMembreId()));
    }

    /**
     * POST /api/v1/matchs/{id}/payer/{membreId}
     * Paiement d'une participation.
     */
    @PostMapping("/{id}/payer/{membreId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ParticipationResponse> payerParticipation(
            @PathVariable Long id,
            @PathVariable Long membreId) {
        return ResponseEntity.ok(matchService.payerParticipation(id, membreId));
    }

}
