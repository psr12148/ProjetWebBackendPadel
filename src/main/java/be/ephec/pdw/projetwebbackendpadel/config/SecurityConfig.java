package be.ephec.pdw.projetwebbackendpadel.config;

import be.ephec.pdw.projetwebbackendpadel.security.CustomUserDetailsService;
import be.ephec.pdw.projetwebbackendpadel.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AppProperties appProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)

                // Stateless — pas de session HTTP
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        // Public — login uniquement
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // Dashboard — ADMIN uniquement
                        .requestMatchers("/api/v1/dashboard/**").hasRole("ADMIN")

                        // Lecture publique (membres connectés), écriture ADMIN
                        .requestMatchers(HttpMethod.GET,    "/api/v1/sites/**").authenticated()
                        .requestMatchers(HttpMethod.GET,    "/api/v1/terrains/**").authenticated()
                        .requestMatchers(HttpMethod.GET,    "/api/v1/membres/**").authenticated()
                        .requestMatchers(HttpMethod.POST,   "/api/v1/sites/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/sites/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/sites/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/terrains/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/terrains/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/terrains/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/membres/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/membres/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/membres/**").hasRole("ADMIN")

                        // Matchs — tous les membres authentifiés
                        .requestMatchers("/api/v1/matchs/**").authenticated()

                        .anyRequest().authenticated()
                )

                // Gestion des erreurs 401 / 403
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(401);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"status\":401,\"message\":\"Non authentifié\"}");
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(403);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"status\":403,\"message\":\"Accès refusé\"}");
                        })
                )

                .authenticationProvider(authenticationProvider())

                // Insère le filtre JWT avant le filtre d'authentification standard
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        // 1. On passe le service directement dans le constructeur
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        // 2. On configure l'encodeur de mot de passe (qui reste un setter classique)
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(
                List.of(appProperties.getCors().getAllowedOrigins()));
        config.setAllowedMethods(
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);   // stateless — pas de cookies
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
