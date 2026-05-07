package be.ephec.pdw.projetwebbackendpadel.service;

import be.ephec.pdw.projetwebbackendpadel.config.AppProperties;
import be.ephec.pdw.projetwebbackendpadel.dto.matchDto.MatchRequest;
import be.ephec.pdw.projetwebbackendpadel.dto.matchDto.MatchResponse;
import be.ephec.pdw.projetwebbackendpadel.dto.participationDto.ParticipationResponse;
import be.ephec.pdw.projetwebbackendpadel.entity.*;
import be.ephec.pdw.projetwebbackendpadel.enums.StatutParticipation;
import be.ephec.pdw.projetwebbackendpadel.enums.TypeMatch;
import be.ephec.pdw.projetwebbackendpadel.enums.StatutMatch;
import be.ephec.pdw.projetwebbackendpadel.exception.BusinessException;
import be.ephec.pdw.projetwebbackendpadel.exception.ResourceNotFoundException;
import be.ephec.pdw.projetwebbackendpadel.mapper.MatchMapper;
import be.ephec.pdw.projetwebbackendpadel.repository.MatchRepository;
import be.ephec.pdw.projetwebbackendpadel.repository.ParticipationRepository;
import be.ephec.pdw.projetwebbackendpadel.repository.TerrainRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchService {

    private final MatchRepository matchRepository;
    private final ParticipationRepository participationRepository;
    private final TerrainRepository terrainRepository;
    private final MembreService membreService;
    private final MatchMapper matchMapper;
    private final AppProperties appProperties;

    // --- Lecture ---

    public MatchResponse findById(Long id){
        return matchMapper.toResponse(getMatchWithParticipations(id));
    }

    public List<MatchResponse> findBySiteAndDate(Long siteId, LocalDate date) {
        return matchRepository.findBySiteAndDate(siteId, date)
                .stream()
                .map(matchMapper::toResponse)
                .toList();
    }

    public List<MatchResponse> findMatchsPublicsDisponibles() {
        return matchRepository.findMatchsPublicsDisponibles(LocalDateTime.now())
                .stream()
                .map(matchMapper::toResponse)
                .toList();
    }

    public List<MatchResponse> findByMembre(Long membreId) {
        return matchRepository.findByMembre(membreId)
                .stream()
                .map(matchMapper::toResponse)
                .toList();
    }


    // --- Création d'un match ---

    /**
     * Crée un match (privé ou public).
     *
     * Règles appliquées :
     * 1. Membre valide (pas de pénalité, pas de solde, délai respecté, bon site)
     * 2. Terrain disponible au créneau demandé
     * 3. Créneau dans les horaires du site
     * 4. Site ouvert ce jour (pas de jour de fermeture)
     * 5. L'organisateur est automatiquement inscrit comme premier joueur
     */
    @Transactional
    public MatchResponse creerMatch(Long organisateurId, MatchRequest request) {
        Membre organisateur = membreService.getMembreOrThrow(organisateurId);
        Terrain terrain = getTerrainOrThrow(request.getTerrainId());
        LocalDate dateMatch = request.getDateHeure().toLocalDate();
        Site site = terrain.getSite();

        // 1. Droits de réservation du membre
        membreService.validerDroitReservation(organisateur, site.getId(), dateMatch);

        // 2. Terrain disponible
        validerTerrainDisponible(terrain.getId(), request.getDateHeure());

        // 3. Créneau dans les horaires du site
        validerCreneauDansSite(site, request.getDateHeure());

        // 4. Site ouvert ce jour
        validerSiteOuvert(site, dateMatch);

        // 5. Créer le match
        Match match = Match.builder()
                .terrain(terrain)
                .organisateur(organisateur)
                .dateHeure(request.getDateHeure())
                .typeMatch(request.getTypeMatch())
                .montantTotal(BigDecimal.valueOf(
                        appProperties.getReservation().getMontantMatchEuros()))
                .build();

        match = matchRepository.save(match);

        // 6. Inscrire l'organisateur comme premier joueur (EN_ATTENTE de paiement)
        Participation participation = Participation.builder()
                .match(match)
                .membre(organisateur)
                .montantDu(BigDecimal.valueOf(
                        appProperties.getReservation().getMontantParJoueur()))
                .build();

        participationRepository.save(participation);

        log.info("Match créé : id={}, type={}, terrain={}, date={}",
                match.getId(), match.getTypeMatch(),
                terrain.getNomAffichage(), request.getDateHeure());

        return matchMapper.toResponse(getMatchWithParticipations(match.getId()));

    }


    // --- Inscription à un match (joueur rejoignant un match public)
    /**
     * Inscrit un membre à un match public.
     *
     * Règles :
     * - Match public uniquement
     * - Match non complet
     * - Membre pas déjà inscrit
     * - Membre valide (droits de réservation)
     * - Match public : l'organisateur ne peut pas inscrire quelqu'un d'autre
     */
    @Transactional
    public ParticipationResponse rejoindreMatchPublic(Long matchId, Long membreId) {
        Match match = getMatchWithParticipations(matchId);
        Membre membre = membreService.getMembreOrThrow(membreId);

        // Vérifications métier
        if (match.getTypeMatch() != TypeMatch.PUBLIC) {
            throw new BusinessException("Seuls les matchs publics sont rejoignables.");
        }
        if (match.estComplet()) {
            throw new BusinessException("Ce match est complet — aucune place disponible.");
        }
        if (match.membreParticipe(membreId)) {
            throw new BusinessException("Vous participez déjà à ce match.");
        }

        // Vérifier droits de réservation
        LocalDate dateMatch = match.getDateHeure().toLocalDate();
        membreService.validerDroitReservation(
                membre, match.getTerrain().getSite().getId(), dateMatch);

        // Créer la participation EN_ATTENTE
        Participation participation = Participation.builder()
                .match(match)
                .membre(membre)
                .montantDu(BigDecimal.valueOf(
                        appProperties.getReservation().getMontantParJoueur()))
                .build();

        participation = participationRepository.save(participation);

        log.info("Membre id={} a rejoint le match id={}", membreId, matchId);
        return matchMapper.toParticipationResponse(participation);
    }


    // --- Ajout d'un joueur à un match privé (par l'organisateur)
    /**
     * L'organisateur ajoute un joueur à son match privé.
     *
     * Règles :
     * - Seul l'organisateur peut ajouter des joueurs dans un match privé
     * - Match non complet
     * - Joueur pas déjà inscrit
     */
    @Transactional
    public ParticipationResponse ajouterJoueurPrive(
            Long matchId, Long organisateurId, Long joueurId) {

        Match match = getMatchWithParticipations(matchId);
        Membre joueur = membreService.getMembreOrThrow(joueurId);

        if (match.getTypeMatch() != TypeMatch.PRIVE) {
            throw new BusinessException(
                    "Cette action est réservée aux matchs privés.");
        }
        if (!match.getOrganisateur().getId().equals(organisateurId)) {
            throw new BusinessException(
                    "Seul l'organisateur peut ajouter des joueurs à un match privé.");
        }
        if (match.estComplet()) {
            throw new BusinessException("Ce match est déjà complet.");
        }
        if (match.membreParticipe(joueurId)) {
            throw new BusinessException("Ce joueur participe déjà à ce match.");
        }

        Participation participation = Participation.builder()
                .match(match)
                .membre(joueur)
                .montantDu(BigDecimal.valueOf(
                        appProperties.getReservation().getMontantParJoueur()))
                .build();

        participation = participationRepository.save(participation);

        log.info("Joueur id={} ajouté au match privé id={} par organisateur id={}",
                joueurId, matchId, organisateurId);

        return matchMapper.toParticipationResponse(participation);
    }


    // --- Paiement d'une participation ---
    /**
     * Enregistre le paiement d'un joueur pour sa participation.
     *
     * Règles :
     * - Si le membre a un solde impayé, il est prélevé en priorité
     * - La participation est confirmée dès paiement complet
     */
    @Transactional
    public ParticipationResponse payerParticipation(Long matchId, Long membreId) {
        Match match = getMatchWithParticipations(matchId);
        Membre membre = membreService.getMembreOrThrow(membreId);
        Participation part = participationRepository
                .findByMatchAndMembre(matchId, membreId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Participation introuvable pour ce match et ce membre."));

        if (part.estPayee()) {
            throw new BusinessException("Vous avez déjà payé pour ce match.");
        }

        BigDecimal montantAPayer = part.getMontantDu();

        // Si le membre a un solde impayé, on l'ajoute au montant à payer
        if (membre.aSoldeImpaye()) {
            montantAPayer = montantAPayer.add(membre.getSoldeImpaye());
            log.info("Solde impayé de {}€ ajouté au paiement du membre id={}",
                    membre.getSoldeImpaye(), membreId);
            membre.reduireSoldeImpaye(membre.getSoldeImpaye());
        }

        part.setMontantPaye(part.getMontantDu());
        part.confirmer();

        log.info("Paiement confirmé : membre id={}, match id={}, montant={}€",
                membreId, matchId, montantAPayer);

        // Vérifier si le match est maintenant complet (4 joueurs confirmés)
        if (match.nombreJoueursConfirmes() + 1 >= 4) {
            match.setStatut(StatutMatch.CONFIRME);
            matchRepository.save(match);
            log.info("Match id={} confirmé — 4 joueurs payés", matchId);
        }

        return matchMapper.toParticipationResponse(participationRepository.save(part));
    }


    // --- Helpers ---

    public Match getMatchWithParticipations(Long id){
        return matchRepository.findByIdWithParticipations(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Match introuvable : id=" + id));
    }

    private Terrain getTerrainOrThrow(Long terrainId) {
        return terrainRepository.findById(terrainId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Terrain introuvable : id=" + terrainId));
    }

    private void validerTerrainDisponible(Long terrainId, LocalDateTime dateHeure) {
        if (!matchRepository.isTerrainDisponible(terrainId, dateHeure)) {
            throw new BusinessException(
                    "Ce terrain est déjà réservé à cette heure.");
        }
    }

    private void validerCreneauDansSite(Site site, LocalDateTime dateHeure) {
        java.time.LocalTime heure = dateHeure.toLocalTime();
        java.time.LocalTime fin   = heure.plusMinutes(
                appProperties.getReservation().getDureeMatchMinutes());

        if (!site.estCreneauValide(heure, fin)) {
            throw new BusinessException(String.format(
                    "Le créneau %s–%s est en dehors des horaires du site (%s–%s).",
                    heure, fin, site.getHeureOuverture(), site.getHeureFermeture()
            ));
        }
    }

    private void validerSiteOuvert(Site site, LocalDate date) {
        boolean ferme = site.getJoursFermeture().stream()
                .anyMatch(j -> date.equals(j.getDate()));
        if (ferme) {
            throw new BusinessException(
                    "Le site est fermé à cette date.");
        }
    }


}
