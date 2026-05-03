package be.ephec.pdw.projetwebbackendpadel.security;

import be.ephec.pdw.projetwebbackendpadel.dto.authDto.LoginRequest;
import be.ephec.pdw.projetwebbackendpadel.dto.membreDto.MembreRequest;
import be.ephec.pdw.projetwebbackendpadel.enums.TypeMembre;
import be.ephec.pdw.projetwebbackendpadel.service.MembreService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class SecurityIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    MembreService membreService;

    private String emailMembre;
    private String emailAdmin;

    @BeforeEach
    void setUp() {
        emailMembre = "membre@test.com";
        emailAdmin  = "admin@padel.be";

        membreService.create(MembreRequest.builder()
                .matricule("G0001").typeMembre(TypeMembre.GLOBAL)
                .nom("Membre").prenom("Test")
                .email(emailMembre).motDePasse("motdepasse123")
                .build());

    }

    /** Helper : login et retourne le token JWT. */
    private String loginEtObtenirToken(String email, String mdp) throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(email, mdp))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(
                result.getResponse().getContentAsString()
        ).get("token").asText();
    }

    @Nested @DisplayName("Login")
    class LoginTests {

        @Test @DisplayName("Login réussi retourne un token JWT")
        void login_retourneToken() throws Exception {
            mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequest(emailMembre, "motdepasse123"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.type").value("Bearer"))
                    .andExpect(jsonPath("$.admin").value(false));
        }

        @Test @DisplayName("Mauvais mot de passe → 401")
        void login_mauvaisMotDePasse() throws Exception {
            mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequest(emailMembre, "mauvaismdp"))))
                    .andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("Admin reconnu via matricule ADMIN")
        void login_admin_reconnu() throws Exception {
            mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequest(emailAdmin, "admin123456"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.admin").value(true));
        }
    }

    @Nested @DisplayName("Autorisations JWT")
    class AutorisationTests {

        @Test @DisplayName("Sans token → 401")
        void sansToken_401() throws Exception {
            mockMvc.perform(get("/v1/sites"))
                    .andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("Token membre → GET /sites autorisé")
        void membreToken_getSites_autorise() throws Exception {
            String token = loginEtObtenirToken(emailMembre, "motdepasse123");

            mockMvc.perform(get("/v1/sites")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("Token membre → POST /sites → 403")
        void membreToken_postSites_403() throws Exception {
            String token = loginEtObtenirToken(emailMembre, "motdepasse123");

            mockMvc.perform(post("/v1/sites")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test @DisplayName("Token admin → GET /dashboard → 200")
        void adminToken_dashboard_200() throws Exception {
            String token = loginEtObtenirToken(emailAdmin, "admin123456");

            mockMvc.perform(get("/v1/dashboard/stats")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("Token membre → GET /dashboard → 403")
        void membreToken_dashboard_403() throws Exception {
            String token = loginEtObtenirToken(emailMembre, "motdepasse123");

            mockMvc.perform(get("/v1/dashboard/stats")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }

        @Test @DisplayName("GET /auth/me retourne le profil du membre connecté")
        void getMe_retourneProfilConnecte() throws Exception {
            String token = loginEtObtenirToken(emailMembre, "motdepasse123");

            mockMvc.perform(get("/v1/auth/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(emailMembre))
                    .andExpect(jsonPath("$.admin").value(false));
        }

        @Test @DisplayName("Token invalide → 401")
        void tokenInvalide_401() throws Exception {
            mockMvc.perform(get("/v1/sites")
                            .header("Authorization", "Bearer token.invalide.ici"))
                    .andExpect(status().isUnauthorized());
        }
    }

}
