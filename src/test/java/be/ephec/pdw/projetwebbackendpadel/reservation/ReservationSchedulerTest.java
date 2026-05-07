package be.ephec.pdw.projetwebbackendpadel.reservation;

import be.ephec.pdw.projetwebbackendpadel.dto.matchDto.*;
import be.ephec.pdw.projetwebbackendpadel.dto.membreDto.*;
import be.ephec.pdw.projetwebbackendpadel.dto.siteDto.*;
import be.ephec.pdw.projetwebbackendpadel.dto.terrainDto.*;
import be.ephec.pdw.projetwebbackendpadel.entity.*;
import be.ephec.pdw.projetwebbackendpadel.enums.*;
import be.ephec.pdw.projetwebbackendpadel.repository.MatchRepository;
import be.ephec.pdw.projetwebbackendpadel.repository.MembreRepository;
import be.ephec.pdw.projetwebbackendpadel.repository.ParticipationRepository;
import be.ephec.pdw.projetwebbackendpadel.scheduler.ReservationScheduler;
import be.ephec.pdw.projetwebbackendpadel.service.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ReservationSchedulerTest {

    @Autowired
    ReservationScheduler scheduler;
    @Autowired
    MatchService matchService;
    @Autowired
    MembreService membreService;
    @Autowired
    SiteService siteService;
    @Autowired
    TerrainService terrainService;
    @Autowired
    MatchRepository matchRepository;
    @Autowired
    MembreRepository membreRepository;
    @Autowired
    ParticipationRepository participationRepository;

    /*
     * EntityManager nécessaire pour vider le cache JPA de premier niveau
     * après l'appel au scheduler. Sans entityManager.clear(), les tests
     * voient les anciennes valeurs des entités modifiées par le scheduler
     * car elles sont mises en cache dans la transaction du test.
     */
    @PersistenceContext
    EntityManager entityManager;

    private Long siteId;
    private Long terrainId;
    private Long organisateurId;
    private Long joueur2Id;
    private Long joueur3Id;
    private Long joueur4Id;

    @BeforeEach
    void setUp() {
        // Site ouvert 24h pour éviter les conflits d'horaires dans les tests
        var site = siteService.create(SiteRequest.builder()
                .nom("Club Scheduler Test")
                .adresse("1 rue du scheduler")
                .nbTerrains(4)
                .heureOuverture(LocalTime.of(0, 0))
                .heureFermeture(LocalTime.of(23, 59))
                .anneeApplicable(LocalDate.now().getYear())
                .build());
        siteId = site.getId();

        var terrain = terrainService.create(TerrainRequest.builder()
                .siteId(siteId).numero(1).build());
        terrainId = terrain.getId();

        organisateurId = creerMembre("G0001", "Org",  "org@test.com");
        joueur2Id      = creerMembre("G0002", "J2",   "j2@test.com");
        joueur3Id      = creerMembre("G0003", "J3",   "j3@test.com");
        joueur4Id      = creerMembre("G0004", "J4",   "j4@test.com");
    }

    // --- Helpers ---
    private Long creerMembre(String matricule, String nom, String email) {
        return membreService.create(MembreRequest.builder()
                .matricule(matricule)
                .typeMembre(TypeMembre.GLOBAL)
                .nom(nom).prenom("Test")
                .email(email).motDePasse("pass12345")
                .build()).getId();
    }

    /**
     * Crée un match dont la date est DEMAIN à l'heure indiquée.
     * Chaque test utilise une heure différente pour éviter
     * les conflits de créneau sur le même terrain.
     */
    private MatchResponse creerMatchDemain(TypeMatch type, int heure) {
        LocalDateTime demain = LocalDateTime.now()
                .plusDays(1)
                .withHour(heure).withMinute(0).withSecond(0).withNano(0);

        return matchService.creerMatch(organisateurId, MatchRequest.builder()
                .terrainId(terrainId)
                .dateHeure(demain)
                .typeMatch(type)
                .build());
    }

    /**
     * Vide le cache JPA pour forcer le rechargement depuis la base.
     * À appeler après scheduler.traiterMatchsDuLendemain() pour que
     * les assertions voient les vraies valeurs en base.
     */
    private void clearCache() {
        entityManager.flush();
        entityManager.clear();
    }

    // --- Étape 1 — Libération des places non payées ---
    @Nested
    @DisplayName("Étape 1 — Libération des places non payées")
    class Etape1 {

        @Test
        @DisplayName("Participation non payée → libérée après le scheduler")
        void participationNonPayee_liberee() {
            var match = creerMatchDemain(TypeMatch.PUBLIC, 10);
            matchService.rejoindreMatchPublic(match.getId(), joueur2Id);
            // joueur2 ne paie pas

            scheduler.traiterMatchsDuLendemain();
            clearCache();  // ← force la relecture depuis la base

            // Vérifie directement via le repository (pas de cache)
            List<Participation> participations = participationRepository
                    .findAll().stream()
                    .filter(p -> p.getMatch().getId().equals(match.getId())
                            && p.getMembre().getId().equals(joueur2Id))
                    .toList();

            assertThat(participations).hasSize(1);
            assertThat(participations.get(0).getStatut())
                    .isEqualTo(StatutParticipation.LIBERE);
        }

        @Test
        @DisplayName("Participation payée → NON libérée")
        void participationPayee_nonLiberee() {
            var match = creerMatchDemain(TypeMatch.PUBLIC, 11);
            matchService.payerParticipation(match.getId(), organisateurId);

            scheduler.traiterMatchsDuLendemain();
            clearCache();

            List<Participation> participations = participationRepository
                    .findAll().stream()
                    .filter(p -> p.getMatch().getId().equals(match.getId())
                            && p.getMembre().getId().equals(organisateurId))
                    .toList();

            assertThat(participations).hasSize(1);
            assertThat(participations.get(0).getStatut())
                    .isEqualTo(StatutParticipation.CONFIRME);
        }
    }


    // --- Bascule privé → public + pénalité organisateur ---
    @Nested
    @DisplayName("Étape 2 — Bascule privé → public")
    class Etape2 {

        @Test
        @DisplayName("Match privé incomplet → bascule en public")
        void matchPriveIncomplet_basculEnPublic() {
            var match = creerMatchDemain(TypeMatch.PRIVE, 12);
            // Seul l'organisateur — incomplet

            scheduler.traiterMatchsDuLendemain();
            clearCache();

            Match updated = matchRepository.findById(match.getId()).orElseThrow();
            assertThat(updated.getTypeMatch()).isEqualTo(TypeMatch.PUBLIC);
        }

        @Test
        @DisplayName("Match privé incomplet → organisateur pénalisé 1 semaine")
        void matchPriveIncomplet_organisateurPenalise() {
            var match = creerMatchDemain(TypeMatch.PRIVE, 13);

            scheduler.traiterMatchsDuLendemain();
            clearCache();

            Membre org = membreRepository.findById(organisateurId).orElseThrow();
            assertThat(org.getPenaliteJusquA()).isNotNull();
            assertThat(org.aPenaliteActive(LocalDate.now())).isTrue();
            // Pénalité = aujourd'hui + 7 jours
            assertThat(org.getPenaliteJusquA())
                    .isEqualTo(LocalDate.now().plusWeeks(1));
        }

        @Test
        @DisplayName("Match privé complet (4 joueurs) → PAS de bascule")
        void matchPriveComplet_pasDeBasculle() {
            var match = creerMatchDemain(TypeMatch.PRIVE, 14);
            matchService.ajouterJoueurPrive(match.getId(), organisateurId, joueur2Id);
            matchService.ajouterJoueurPrive(match.getId(), organisateurId, joueur3Id);
            matchService.ajouterJoueurPrive(match.getId(), organisateurId, joueur4Id);

            scheduler.traiterMatchsDuLendemain();
            clearCache();

            Match updated = matchRepository.findById(match.getId()).orElseThrow();
            assertThat(updated.getTypeMatch()).isEqualTo(TypeMatch.PRIVE);

            // Pas de pénalité
            Membre org = membreRepository.findById(organisateurId).orElseThrow();
            assertThat(org.getPenaliteJusquA()).isNull();
        }
    }


    // --- Solde organisateur match public incomplet ---
    @Nested
    @DisplayName("Étape 3 — Solde organisateur match public")
    class Etape3 {

        @Test
        @DisplayName("1 joueur payé sur 4 → organisateur doit 3 × 15€ = 45€")
        void matchPublicIncomplet_solde45euros() {
            var match = creerMatchDemain(TypeMatch.PUBLIC, 15);
            matchService.payerParticipation(match.getId(), organisateurId);
            // 3 places vides

            scheduler.traiterMatchsDuLendemain();
            clearCache();

            Membre org = membreRepository.findById(organisateurId).orElseThrow();
            assertThat(org.getSoldeImpaye())
                    .isEqualByComparingTo(BigDecimal.valueOf(45));
        }

        @Test
        @DisplayName("2 joueurs payés sur 4 → organisateur doit 2 × 15€ = 30€")
        void matchPublicDeuxJoueursPayes_solde30euros() {
            var match = creerMatchDemain(TypeMatch.PUBLIC, 16);
            matchService.rejoindreMatchPublic(match.getId(), joueur2Id);
            matchService.payerParticipation(match.getId(), organisateurId);
            matchService.payerParticipation(match.getId(), joueur2Id);
            // 2 places vides

            scheduler.traiterMatchsDuLendemain();
            clearCache();

            Membre org = membreRepository.findById(organisateurId).orElseThrow();
            assertThat(org.getSoldeImpaye())
                    .isEqualByComparingTo(BigDecimal.valueOf(30));
        }

        @Test
        @DisplayName("Match public complet → pas de solde pour l'organisateur")
        void matchPublicComplet_pasDeSolde() {
            var match = creerMatchDemain(TypeMatch.PUBLIC, 17);
            matchService.rejoindreMatchPublic(match.getId(), joueur2Id);
            matchService.rejoindreMatchPublic(match.getId(), joueur3Id);
            matchService.rejoindreMatchPublic(match.getId(), joueur4Id);

            matchService.payerParticipation(match.getId(), organisateurId);
            matchService.payerParticipation(match.getId(), joueur2Id);
            matchService.payerParticipation(match.getId(), joueur3Id);
            matchService.payerParticipation(match.getId(), joueur4Id);

            scheduler.traiterMatchsDuLendemain();
            clearCache();

            Membre org = membreRepository.findById(organisateurId).orElseThrow();
            assertThat(org.getSoldeImpaye())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }


    // --- Confirmation du match ---
    @Nested
    @DisplayName("Étape 4 — Confirmation du match")
    class Etape4 {

        @Test
        @DisplayName("4 joueurs payés → match CONFIRME")
        void quatreJoueursPayes_matchConfirme() {
            var match = creerMatchDemain(TypeMatch.PUBLIC, 18);
            matchService.rejoindreMatchPublic(match.getId(), joueur2Id);
            matchService.rejoindreMatchPublic(match.getId(), joueur3Id);
            matchService.rejoindreMatchPublic(match.getId(), joueur4Id);

            matchService.payerParticipation(match.getId(), organisateurId);
            matchService.payerParticipation(match.getId(), joueur2Id);
            matchService.payerParticipation(match.getId(), joueur3Id);
            matchService.payerParticipation(match.getId(), joueur4Id);

            scheduler.traiterMatchsDuLendemain();
            clearCache();

            Match updated = matchRepository.findById(match.getId()).orElseThrow();
            assertThat(updated.getStatut()).isEqualTo(StatutMatch.CONFIRME);
        }

        @Test
        @DisplayName("Moins de 4 joueurs → match reste EN_ATTENTE")
        void moinsDeQuatreJoueurs_resteEnAttente() {
            var match = creerMatchDemain(TypeMatch.PUBLIC, 19);
            matchService.payerParticipation(match.getId(), organisateurId);
            // Seulement 1 joueur payé

            scheduler.traiterMatchsDuLendemain();
            clearCache();

            Match updated = matchRepository.findById(match.getId()).orElseThrow();
            assertThat(updated.getStatut()).isEqualTo(StatutMatch.EN_ATTENTE);
        }
    }


    // --- Scénario complet end-to-end ---
    @Nested
    @DisplayName("Scénario complet")
    class ScenarioComplet {

        @Test
        @DisplayName("Match privé incomplet → public + pénalité + solde en cascade")
        void scenarioComplet_matchPriveIncomplet() {
            // Match privé avec seulement l'organisateur
            var match = creerMatchDemain(TypeMatch.PRIVE, 20);

            scheduler.traiterMatchsDuLendemain();
            clearCache();

            Match updatedMatch = matchRepository.findById(match.getId()).orElseThrow();
            Membre updatedOrg = membreRepository.findById(organisateurId).orElseThrow();

            // 1. Le match a basculé en public
            assertThat(updatedMatch.getTypeMatch()).isEqualTo(TypeMatch.PUBLIC);

            // 2. L'organisateur a une pénalité active
            assertThat(updatedOrg.aPenaliteActive(LocalDate.now())).isTrue();

            // 3. L'organisateur a un solde impayé > 0
            // (3 places vides × 15€ = 45€, mais l'org n'a pas payé sa propre part non plus)
            assertThat(updatedOrg.getSoldeImpaye())
                    .isGreaterThan(BigDecimal.ZERO);
        }
    }
}
