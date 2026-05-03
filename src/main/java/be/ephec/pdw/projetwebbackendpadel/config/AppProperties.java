package be.ephec.pdw.projetwebbackendpadel.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "padel")
public class AppProperties {
    private Reservation reservation = new Reservation();
    private Scheduler scheduler = new Scheduler();
    private Cors cors = new Cors();
    private Security security = new Security();

    @Getter
    @Setter
    public static class Reservation {
        @Positive
        private int dureeMatchMinutes = 90;

        @Positive
        private int pauseEntreMatchsMinutes = 15;

        @Positive
        private int montantMatchEuros = 60;

        @Positive
        private int delaiGlobalSemaines = 3;

        @Positive
        private int delaiSiteSemaines = 2;

        @Positive
        private int delaiLibreJours = 5;

        /** Montant par joueur = montantMatchEuros / 4 */
        public int getMontantParJoueur() {
            return montantMatchEuros / 4;
        }
    }

    @Getter
    @Setter
    public static class Scheduler {

        @NotBlank
        private String cronJ1 = "0 0 20 * * *";
    }

    @Getter
    @Setter
    public static class Cors {

        @NotBlank
        private String allowedOrigins = "http://localhost:4200";
    }

    @Getter
    @Setter
    public static class Security {
        /**
         * Clé secrète HMAC-SHA512 — minimum 64 caractères.
         * À surcharger via variable d'environnement PADEL_SECURITY_JWT-SECRET en prod.
         */
        @NotBlank
        private String jwtSecret = "ephec-pdw-super-secret-jwt-signing-key-must-be-at-least-256-bits!!";

        /** Durée de validité du token en millisecondes */
        @Positive
        private long jwtExpirationMs = 900000L;
    }

}
