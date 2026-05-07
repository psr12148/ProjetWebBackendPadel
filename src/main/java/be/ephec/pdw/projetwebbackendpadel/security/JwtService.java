package be.ephec.pdw.projetwebbackendpadel.security;

import be.ephec.pdw.projetwebbackendpadel.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    // --- Génération ---

    /**
     * Génère un token JWT pour un utilisateur authentifié.
     * Claims inclus : membreId, matricule, admin.
     */
    public String generateToken(CustomUserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("membreId",  userDetails.getMembreId());
        claims.put("matricule", userDetails.getMatricule());
        claims.put("admin",     userDetails.isAdmin());

        return buildToken(claims, userDetails.getUsername(),
                jwtProperties.getExpiration());
    }

    public String generateRefreshToken(CustomUserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails.getUsername(),
                jwtProperties.getRefreshExpiration());
    }

    private String buildToken(Map<String, Object> claims, String subject, long expiration) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }


    // --- Validation ---

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String email = extractEmail(token);
            return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            log.warn("Token JWT invalide : {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }


    // --- Extraction des claims ---

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Long extractMembreId(String token) {
        return extractAllClaims(token).get("membreId", Long.class);
    }

    public String extractMatricule(String token) {
        return extractAllClaims(token).get("matricule", String.class);
    }

    public boolean extractIsAdmin(String token) {
        return extractAllClaims(token).get("admin", Boolean.class);
    }

    private Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    // --- Clé de signature ---

    private SecretKey getSigningKey() {
        // Utilise les bytes UTF-8 de la clé directement
        return Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

}
