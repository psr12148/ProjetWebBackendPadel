package be.ephec.pdw.projetwebbackendpadel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Active :
 * - JPA Auditing (@CreatedDate, @LastModifiedDate, @CreatedBy, @LastModifiedBy)
 * - Scan des repositories dans le package repository
 */

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableJpaRepositories(basePackages = "be.ephec.pdw.projetwebbackendpadel")
public class JpaConfig {
    /**
     * Fournit l'identifiant de l'utilisateur connecté pour @CreatedBy / @LastModifiedBy.
     * Retourne "system" si aucun utilisateur n'est authentifié (scheduler, tests).
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return Optional.of("system");
            }
            return Optional.of(auth.getName());
        };
    }
}
