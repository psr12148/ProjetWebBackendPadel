package be.ephec.pdw.projetwebbackendpadel.controller;

import be.ephec.pdw.projetwebbackendpadel.dto.authDto.AuthResponse;
import be.ephec.pdw.projetwebbackendpadel.dto.authDto.LoginRequest;
import be.ephec.pdw.projetwebbackendpadel.entity.Membre;
import be.ephec.pdw.projetwebbackendpadel.security.CustomUserDetails;
import be.ephec.pdw.projetwebbackendpadel.security.JwtService;
import be.ephec.pdw.projetwebbackendpadel.service.MembreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final MembreService membreService;

    /**
     * POST /api/v1/auth/login
     * Retourne un token JWT à stocker côté Angular (localStorage).
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getMotDePasse()
                    )
            );

            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            String            token       = jwtService.generateToken(userDetails);
            Membre membre      = membreService.getMembreOrThrow(
                    userDetails.getMembreId());

            log.info("Login réussi : email={}, admin={}",
                    request.getEmail(), userDetails.isAdmin());

            return ResponseEntity.ok(AuthResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .membreId(userDetails.getMembreId())
                    .matricule(userDetails.getMatricule())
                    .email(userDetails.getEmail())
                    .nom(membre.getNom())
                    .prenom(membre.getPrenom())
                    .typeLabel(switch (membre.getTypeMembre()) {
                        case GLOBAL -> "Membre Global";
                        case SITE   -> "Membre Site";
                        case LIBRE  -> "Membre Libre";
                    })
                    .admin(userDetails.isAdmin())
                    .build());

        } catch (BadCredentialsException e) {
            log.warn("Échec login : {}", request.getEmail());
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Email ou mot de passe incorrect"));
        }
    }

    /**
     * GET /api/v1/auth/me
     * Retourne les infos du membre authentifié via son token.
     */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Membre membre = membreService.getMembreOrThrow(userDetails.getMembreId());

        return ResponseEntity.ok(AuthResponse.builder()
                .membreId(userDetails.getMembreId())
                .matricule(userDetails.getMatricule())
                .email(userDetails.getEmail())
                .nom(membre.getNom())
                .prenom(membre.getPrenom())
                .typeLabel(switch (membre.getTypeMembre()) {
                    case GLOBAL -> "Membre Global";
                    case SITE   -> "Membre Site";
                    case LIBRE  -> "Membre Libre";
                })
                .admin(userDetails.isAdmin())
                .build());
    }

    /**
     * POST /api/v1/auth/logout
     * JWT étant stateless, le logout se fait côté client
     * en supprimant le token du localStorage.
     * Cet endpoint confirme juste la déconnexion.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "Déconnexion réussie"));
    }

}
