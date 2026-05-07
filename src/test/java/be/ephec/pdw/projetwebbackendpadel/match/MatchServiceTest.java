package be.ephec.pdw.projetwebbackendpadel.match;

import be.ephec.pdw.projetwebbackendpadel.config.AppProperties;
import be.ephec.pdw.projetwebbackendpadel.dto.matchDto.MatchRequest;
import be.ephec.pdw.projetwebbackendpadel.dto.matchDto.MatchResponse;
import be.ephec.pdw.projetwebbackendpadel.dto.participationDto.ParticipationResponse;
import be.ephec.pdw.projetwebbackendpadel.entity.*;
import be.ephec.pdw.projetwebbackendpadel.enums.StatutMatch;
import be.ephec.pdw.projetwebbackendpadel.enums.StatutParticipation;
import be.ephec.pdw.projetwebbackendpadel.enums.TypeMatch;
import be.ephec.pdw.projetwebbackendpadel.exception.BusinessException;
import be.ephec.pdw.projetwebbackendpadel.exception.ResourceNotFoundException;
import be.ephec.pdw.projetwebbackendpadel.mapper.MatchMapper;
import be.ephec.pdw.projetwebbackendpadel.repository.MatchRepository;
import be.ephec.pdw.projetwebbackendpadel.repository.ParticipationRepository;
import be.ephec.pdw.projetwebbackendpadel.repository.TerrainRepository;
import be.ephec.pdw.projetwebbackendpadel.service.MatchService;
import be.ephec.pdw.projetwebbackendpadel.service.MembreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MatchServiceTest {

    @Mock private MatchRepository matchRepository;
    @Mock private ParticipationRepository participationRepository;
    @Mock private TerrainRepository terrainRepository;
    @Mock private MembreService membreService;
    @Mock private MatchMapper matchMapper;
    @Mock private AppProperties appProperties;

    @InjectMocks private MatchService matchService;

    // Données de test partagées
    private Membre organisateur;
    private Membre joueur2;
    private Site site;
    private Terrain terrain;
    private AppProperties.Reservation reservation;


    @BeforeEach
    void setUp() {
        organisateur = new Membre();
        organisateur.setId(1L);
        organisateur.setMatricule("G0001");
        organisateur.setSoldeImpaye(BigDecimal.ZERO);

        joueur2 = new Membre();
        joueur2.setId(2L);
        joueur2.setMatricule("G0002");
        joueur2.setSoldeImpaye(BigDecimal.ZERO);

        site = new Site();
        site.setId(1L);
        site.setHeureOuverture(LocalTime.of(8, 0));
        site.setHeureFermeture(LocalTime.of(22, 0));
        site.setJoursFermeture(new ArrayList<>());

        terrain = new Terrain();
        terrain.setId(1L);
        terrain.setNumero(1);
        terrain.setSite(site);

        // Configuration AppProperties partagée
        reservation = new AppProperties.Reservation();
        reservation.setMontantMatchEuros(60);
        reservation.setDureeMatchMinutes(90);
    }


    // --- Creer un match ---

    @Test
    @DisplayName("creerMatch : succès → retourne un MatchResponse")
    void creerMatch_doitCreerMatch_quandToutEstValide() {
        // Arrange
        LocalDateTime dateMatch = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
        MatchRequest request = new MatchRequest();
        request.setTerrainId(1L);
        request.setDateHeure(dateMatch);
        request.setTypeMatch(TypeMatch.PUBLIC);

        Match savedMatch = new Match();
        savedMatch.setId(100L);
        savedMatch.setTerrain(terrain);
        savedMatch.setOrganisateur(organisateur);
        savedMatch.setParticipations(new ArrayList<>());

        MatchResponse response = new MatchResponse();
        response.setId(100L);

        when(membreService.getMembreOrThrow(1L)).thenReturn(organisateur);
        when(terrainRepository.findById(1L)).thenReturn(Optional.of(terrain));
        when(matchRepository.isTerrainDisponible(1L, dateMatch)).thenReturn(true);
        when(appProperties.getReservation()).thenReturn(reservation);
        when(matchRepository.save(any(Match.class))).thenReturn(savedMatch);
        when(matchRepository.findByIdWithParticipations(100L))
                .thenReturn(Optional.of(savedMatch));
        when(matchMapper.toResponse(savedMatch)).thenReturn(response);

        // Act
        MatchResponse resultat = matchService.creerMatch(1L, request);

        // Assert
        assertNotNull(resultat);
        assertEquals(100L, resultat.getId());
        verify(matchRepository, times(1)).save(any(Match.class));
        verify(participationRepository, times(1)).save(any(Participation.class));
    }

    @Test
    @DisplayName("creerMatch : terrain déjà réservé → BusinessException")
    void creerMatch_doitLancerException_quandTerrainIndisponible() {
        // Arrange
        LocalDateTime dateMatch = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
        MatchRequest request = new MatchRequest();
        request.setTerrainId(1L);
        request.setDateHeure(dateMatch);
        request.setTypeMatch(TypeMatch.PUBLIC);

        when(membreService.getMembreOrThrow(1L)).thenReturn(organisateur);
        when(terrainRepository.findById(1L)).thenReturn(Optional.of(terrain));
        when(matchRepository.isTerrainDisponible(1L, dateMatch)).thenReturn(false);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> matchService.creerMatch(1L, request));

        assertTrue(ex.getMessage().contains("déjà réservé"));
        verify(matchRepository, never()).save(any(Match.class));
        verify(participationRepository, never()).save(any(Participation.class));
    }

    @Test
    @DisplayName("creerMatch : créneau hors horaires du site → BusinessException")
    void creerMatch_doitLancerException_quandCreneauHorsHoraires() {
        // Arrange — match à 23h, site ferme à 22h
        LocalDateTime dateMatch = LocalDateTime.now().plusDays(2).withHour(23).withMinute(0);
        MatchRequest request = new MatchRequest();
        request.setTerrainId(1L);
        request.setDateHeure(dateMatch);
        request.setTypeMatch(TypeMatch.PUBLIC);

        when(membreService.getMembreOrThrow(1L)).thenReturn(organisateur);
        when(terrainRepository.findById(1L)).thenReturn(Optional.of(terrain));
        when(matchRepository.isTerrainDisponible(1L, dateMatch)).thenReturn(true);
        when(appProperties.getReservation()).thenReturn(reservation);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> matchService.creerMatch(1L, request));

        assertTrue(ex.getMessage().contains("dehors des horaires"));
        verify(matchRepository, never()).save(any(Match.class));
    }


    // --- Rejoindre un match public ---

    @Test
    @DisplayName("rejoindreMatchPublic : succès → crée une Participation")
    void rejoindreMatchPublic_doitInscrireMembre_quandValide() {
        // Arrange
        Match match = creerMatchPublicVide();
        Participation savedPart = new Participation();
        savedPart.setId(50L);
        savedPart.setMembre(joueur2);
        savedPart.setMatch(match);

        ParticipationResponse response = new ParticipationResponse();
        response.setId(50L);

        when(matchRepository.findByIdWithParticipations(10L))
                .thenReturn(Optional.of(match));
        when(membreService.getMembreOrThrow(2L)).thenReturn(joueur2);
        when(appProperties.getReservation()).thenReturn(reservation);
        when(participationRepository.save(any(Participation.class))).thenReturn(savedPart);
        when(matchMapper.toParticipationResponse(savedPart)).thenReturn(response);

        // Act
        ParticipationResponse resultat = matchService.rejoindreMatchPublic(10L, 2L);

        // Assert
        assertNotNull(resultat);
        assertEquals(50L, resultat.getId());
        verify(participationRepository, times(1)).save(any(Participation.class));
    }

    @Test
    @DisplayName("rejoindreMatchPublic : match privé → BusinessException")
    void rejoindreMatchPublic_doitLancerException_quandMatchPrive() {
        // Arrange
        Match matchPrive = creerMatchPublicVide();
        matchPrive.setTypeMatch(TypeMatch.PRIVE);  // ← privé

        when(matchRepository.findByIdWithParticipations(10L))
                .thenReturn(Optional.of(matchPrive));
        when(membreService.getMembreOrThrow(2L)).thenReturn(joueur2);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> matchService.rejoindreMatchPublic(10L, 2L));

        assertTrue(ex.getMessage().contains("publics"));
        verify(participationRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejoindreMatchPublic : match complet → BusinessException")
    void rejoindreMatchPublic_doitLancerException_quandMatchComplet() {
        // Arrange — match avec 4 joueurs déjà inscrits
        Match match = creerMatchPublicVide();
        for (int i = 0; i < 4; i++) {
            Participation p = new Participation();
            p.setStatut(StatutParticipation.EN_ATTENTE);
            Membre m = new Membre();
            m.setId(100L + i);
            p.setMembre(m);
            match.getParticipations().add(p);
        }

        when(matchRepository.findByIdWithParticipations(10L))
                .thenReturn(Optional.of(match));
        when(membreService.getMembreOrThrow(2L)).thenReturn(joueur2);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> matchService.rejoindreMatchPublic(10L, 2L));

        assertTrue(ex.getMessage().contains("complet"));
        verify(participationRepository, never()).save(any());
    }


    // --- ajouter un joueur à un match privé ---

    @Test
    @DisplayName("ajouterJoueurPrive : organisateur ajoute joueur → succès")
    void ajouterJoueurPrive_doitAjouterJoueur_quandOrganisateur() {
        // Arrange
        Match matchPrive = creerMatchPublicVide();
        matchPrive.setTypeMatch(TypeMatch.PRIVE);

        Participation savedPart = new Participation();
        savedPart.setId(60L);

        ParticipationResponse response = new ParticipationResponse();
        response.setId(60L);

        when(matchRepository.findByIdWithParticipations(10L))
                .thenReturn(Optional.of(matchPrive));
        when(membreService.getMembreOrThrow(2L)).thenReturn(joueur2);
        when(appProperties.getReservation()).thenReturn(reservation);
        when(participationRepository.save(any(Participation.class))).thenReturn(savedPart);
        when(matchMapper.toParticipationResponse(savedPart)).thenReturn(response);

        // Act
        ParticipationResponse resultat = matchService.ajouterJoueurPrive(10L, 1L, 2L);

        // Assert
        assertNotNull(resultat);
        assertEquals(60L, resultat.getId());
        verify(participationRepository, times(1)).save(any(Participation.class));
    }

    @Test
    @DisplayName("ajouterJoueurPrive : non-organisateur → BusinessException")
    void ajouterJoueurPrive_doitLancerException_quandPasOrganisateur() {
        // Arrange — l'organisateur du match est id=1, on essaie avec id=99
        Match matchPrive = creerMatchPublicVide();
        matchPrive.setTypeMatch(TypeMatch.PRIVE);

        when(matchRepository.findByIdWithParticipations(10L))
                .thenReturn(Optional.of(matchPrive));
        when(membreService.getMembreOrThrow(2L)).thenReturn(joueur2);

        // Act & Assert — id=99 n'est pas l'organisateur (qui est id=1)
        BusinessException ex = assertThrows(BusinessException.class,
                () -> matchService.ajouterJoueurPrive(10L, 99L, 2L));

        assertTrue(ex.getMessage().contains("organisateur"));
        verify(participationRepository, never()).save(any());
    }


    // --- payer la participation ---

    @Test
    @DisplayName("payerParticipation : succès → confirme la participation")
    void payerParticipation_doitConfirmer_quandPaiementValide() {
        // Arrange
        Match match = creerMatchPublicVide();
        Participation part = new Participation();
        part.setId(70L);
        part.setMatch(match);
        part.setMembre(organisateur);
        part.setMontantDu(BigDecimal.valueOf(15));
        part.setMontantPaye(BigDecimal.ZERO);
        part.setStatut(StatutParticipation.EN_ATTENTE);

        ParticipationResponse response = new ParticipationResponse();
        response.setId(70L);

        when(matchRepository.findByIdWithParticipations(10L))
                .thenReturn(Optional.of(match));
        when(membreService.getMembreOrThrow(1L)).thenReturn(organisateur);
        when(participationRepository.findByMatchAndMembre(10L, 1L))
                .thenReturn(Optional.of(part));
        when(participationRepository.save(any(Participation.class))).thenReturn(part);
        when(matchMapper.toParticipationResponse(part)).thenReturn(response);

        // Act
        ParticipationResponse resultat = matchService.payerParticipation(10L, 1L);

        // Assert
        assertNotNull(resultat);
        assertEquals(StatutParticipation.CONFIRME, part.getStatut());
        assertEquals(0, part.getMontantPaye().compareTo(BigDecimal.valueOf(15)));
        verify(participationRepository, times(1)).save(part);
    }

    @Test
    @DisplayName("payerParticipation : déjà payée → BusinessException")
    void payerParticipation_doitLancerException_quandDejaPayee() {
        // Arrange
        Match match = creerMatchPublicVide();
        Participation part = new Participation();
        part.setId(70L);
        part.setMatch(match);
        part.setMembre(organisateur);
        part.setMontantDu(BigDecimal.valueOf(15));
        part.setMontantPaye(BigDecimal.valueOf(15));  // ← déjà payé
        part.setStatut(StatutParticipation.CONFIRME);

        when(matchRepository.findByIdWithParticipations(10L))
                .thenReturn(Optional.of(match));
        when(membreService.getMembreOrThrow(1L)).thenReturn(organisateur);
        when(participationRepository.findByMatchAndMembre(10L, 1L))
                .thenReturn(Optional.of(part));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> matchService.payerParticipation(10L, 1L));

        assertTrue(ex.getMessage().contains("déjà payé"));
        verify(participationRepository, never()).save(any());
    }


    // --- Helpers ---

    /**
     * Crée un match public vide (organisateur seulement, pas de participations).
     */
    private Match creerMatchPublicVide() {
        Match match = new Match();
        match.setId(10L);
        match.setTerrain(terrain);
        match.setOrganisateur(organisateur);
        match.setDateHeure(LocalDateTime.now().plusDays(2).withHour(10));
        match.setTypeMatch(TypeMatch.PUBLIC);
        match.setStatut(StatutMatch.EN_ATTENTE);
        match.setParticipations(new ArrayList<>());
        return match;
    }


}
