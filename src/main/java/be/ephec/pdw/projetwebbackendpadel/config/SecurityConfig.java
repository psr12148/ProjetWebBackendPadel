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
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
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
    private final JwtAuthenticationFilter  jwtAuthFilter;
    private final AppProperties            appProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)

                // Nécessaire pour que la console H2 s'affiche (iframe)
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        // ===== Endpoints publics =====

                        // Console H2 — dev uniquement
                        .requestMatchers("/h2-console/**").permitAll()

                        // Swagger / OpenAPI
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // Actuator health
                        .requestMatchers("/actuator/health").permitAll()

                        // Auth — login sans token
                        // ATTENTION : context-path = /api, donc les controllers mappent /v1/...
                        // Spring Security intercepte AVANT le context-path → pas de /api/ ici
                        .requestMatchers("/v1/auth/**").permitAll()

                        // ===== Dashboard — ADMIN uniquement =====
                        .requestMatchers("/v1/dashboard/**").hasRole("ADMIN")

                        // ===== Sites — lecture authentifiée, écriture ADMIN =====
                        .requestMatchers(HttpMethod.GET,    "/v1/sites/**").authenticated()
                        .requestMatchers(HttpMethod.POST,   "/v1/sites/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/v1/sites/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/v1/sites/**").hasRole("ADMIN")

                        // ===== Terrains =====
                        .requestMatchers(HttpMethod.GET,    "/v1/terrains/**").authenticated()
                        .requestMatchers(HttpMethod.POST,   "/v1/terrains/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/v1/terrains/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/v1/terrains/**").hasRole("ADMIN")

                        // ===== Membres =====
                        .requestMatchers(HttpMethod.GET,    "/v1/membres/**").authenticated()
                        .requestMatchers(HttpMethod.POST,   "/v1/membres/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/v1/membres/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/v1/membres/**").hasRole("ADMIN")

                        // ===== Matchs — tous les membres connectés =====
                        .requestMatchers("/v1/matchs/**").authenticated()

                        .anyRequest().authenticated()
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(401);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write(
                                    "{\"status\":401,\"message\":\"Non authentifié\"}");
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(403);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write(
                                    "{\"status\":403,\"message\":\"Accès refusé\"}");
                        })
                )

                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);
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
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
