package be.ephec.pdw.projetwebbackendpadel.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Propriétés JWT — préfixe "jwt.*" conformément à application.properties.
 *
 * Injectables dans JwtService via @RequiredArgsConstructor.
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    @NotBlank
    private String secret =
            "ephec-pdw-super-secret-jwt-signing-key-must-be-at-least-256-bits!!";

    /** Durée de validité du token en ms (défaut : 15 min). */
    @Positive
    private long expiration = 900000L;

    /** Durée de validité du refresh token en ms (défaut : 7 jours). */
    @Positive
    private long refreshExpiration = 604800000L;
}
