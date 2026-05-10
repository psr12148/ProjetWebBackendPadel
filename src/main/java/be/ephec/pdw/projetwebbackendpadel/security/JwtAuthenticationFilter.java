package be.ephec.pdw.projetwebbackendpadel.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre JWT — s'exécute une fois par requête.
 *
 * Logique :
 * 1. Lit le header "Authorization: Bearer <token>"
 * 2. Valide le token via JwtService
 * 3. Charge l'utilisateur depuis CustomUserDetailsService
 * 4. Injecte l'authentification dans le SecurityContext
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Ignore les requêtes sans token Bearer
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7); // "Bearer " = 7 chars

        try {
            final String email = jwtService.extractEmail(token);

            // Authentifie seulement si pas déjà authentifié
            if (StringUtils.hasText(email)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                CustomUserDetails userDetails =
                        (CustomUserDetails) userDetailsService.loadUserByUsername(email);

                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            log.warn("Erreur JWT sur {} : {}", request.getRequestURI(), e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Ne filtre pas les endpoints d'authentification publics (login, logout)
     * qui n'ont PAS besoin de token JWT.
     *
     * IMPORTANT : /v1/auth/me a besoin du filtre car il utilise
     * @AuthenticationPrincipal qui dépend du SecurityContext rempli par
     * ce filtre. Si on l'exclut, userDetails est null → NullPointerException.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // CORRECTION : exclusion ciblée des routes publiques uniquement
        return path.equals("/v1/auth/login")
                || path.equals("/v1/auth/logout");
    }

}
