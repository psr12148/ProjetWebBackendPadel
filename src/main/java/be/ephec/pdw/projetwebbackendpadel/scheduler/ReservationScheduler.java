package be.ephec.pdw.projetwebbackendpadel.scheduler;

import be.ephec.pdw.projetwebbackendpadel.config.AppProperties;
import be.ephec.pdw.projetwebbackendpadel.entity.Match;
import be.ephec.pdw.projetwebbackendpadel.entity.Membre;
import be.ephec.pdw.projetwebbackendpadel.entity.Participation;
import be.ephec.pdw.projetwebbackendpadel.enums.StatutMatch;
import be.ephec.pdw.projetwebbackendpadel.enums.StatutParticipation;
import be.ephec.pdw.projetwebbackendpadel.enums.TypeMatch;
import be.ephec.pdw.projetwebbackendpadel.repository.MatchRepository;
import be.ephec.pdw.projetwebbackendpadel.repository.MembreRepository;
import be.ephec.pdw.projetwebbackendpadel.repository.ParticipationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationScheduler {

    private final MatchRepository matchRepository;
    private final ParticipationRepository participationRepository;
    private final MembreRepository      membreRepository;
    private final AppProperties         appProperties;

    // --- Point d'entrée - déclenché par le cron ---

    @Scheduled(cron = "${padel.scheduler.cron-j1}")
    @Transactional
    public void traiterMatchsDuLendemain() {
        LocalDate demain = LocalDate.now().plusDays(1);
        log.info("=== Scheduler J-1 démarré pour le {} ===", demain);

        List<Match> matchsDemain = matchRepository.findEnAttenteForDemain(demain);
        log.info("Matchs EN_ATTENTE trouvés pour demain : {}", matchsDemain.size());

        for (Match match : matchsDemain) {
            try {
                traiterMatch(match);
            } catch (Exception e) {
                log.error("Erreur lors du traitement du match id={} : {}",
                        match.getId(), e.getMessage(), e);
            }
        }

        log.info("=== Scheduler J-1 terminé ===");
    }


    // --- Traitement d'un match ---

    private void traiterMatch(Match match) {
        log.info("Traitement match id={}, type={}, statut={}",
                match.getId(), match.getTypeMatch(), match.getStatut());

        /*
         * ÉTAPE 2 d'abord pour les matchs PRIVÉS :
         *
         * On doit évaluer si le match privé est complet AVANT
         * de libérer les places non payées (étape 1).
         *
         * Règle métier : si un match privé a 4 joueurs inscrits
         * (même non payés), il ne bascule pas en public.
         * Ce sont les matchs où il manque des joueurs (< 4 inscrits)
         * qui basculent. Les joueurs non payés seront libérés
         * APRÈS la bascule éventuelle.
         *
         * Ordre correct : étape 2 → étape 1 → étape 3 → étape 4
         */
        if (match.getTypeMatch() == TypeMatch.PRIVE) {
            etape2_basculerPriveEnPublicSiIncomplet(match);
            // Rechargement pour que l'étape 1 voie le bon typeMatch
            match = rechargerMatch(match.getId());
        }

        // ÉTAPE 1 : Libérer les places non payées
        etape1_libererPlacesNonPayees(match);

        /*
         * IMPORTANT : après l'étape 1, les participations ont été modifiées
         * en base (statut → LIBERE). Pour que les étapes suivantes voient
         * l'état à jour, on recharge le match depuis la base.
         * Sans ce rechargement, le cache JPA de premier niveau retourne
         * encore les anciennes valeurs et les étapes 2, 3, 4 calculent
         * des résultats incorrects.
         */
        match = rechargerMatch(match.getId());


        // ÉTAPE 3 : Solde organisateur si match public incomplet
        if (match.getTypeMatch() == TypeMatch.PUBLIC) {
            etape3_calculerSoldeOrganisateur(match);
        }

        // ÉTAPE 4 : Confirmer si 4 joueurs confirmés
        etape4_confirmerMatchComplet(match);
    }


    // --- Libération des places non payées ---

    private void etape1_libererPlacesNonPayees(Match match) {
        List<Participation> nonPayees =
                participationRepository.findNonPayeesForMatch(match.getId());

        for (Participation p : nonPayees) {
            p.liberer();
            participationRepository.save(p);
            log.info("[Étape 1] Place libérée — match id={}, membre id={} (non payé)",
                    match.getId(), p.getMembre().getId());
        }
    }



    // --- Bascule privé -> public + pénalité organisateur ---

    private void etape2_basculerPriveEnPublicSiIncomplet(Match match) {
        /*
         * On compte les joueurs ACTIFS (non libérés) depuis la base.
         * À ce stade (avant l'étape 1), les places non payées
         * sont encore EN_ATTENTE — donc "actives".
         *
         * Si le nombre total d'inscrits actifs < 4 → bascule.
         * Si 4 joueurs sont inscrits (même non payés) → pas de bascule,
         * leurs places seront libérées à l'étape 1 si non payées.
         */
        long joueursInscrits = participationRepository
                .findActivesForMatch(match.getId()).size();

        if (joueursInscrits >= 4) {
            log.info("[Étape 2] Match privé id={} : {} joueur(s) inscrit(s), pas de bascule",
                    match.getId(), joueursInscrits);
            return;
        }

        // Bascule en public
        match.setTypeMatch(TypeMatch.PUBLIC);
        matchRepository.save(match);

        log.warn("Match id={} basculé de PRIVE à PUBLIC ({} joueur(s) actif(s))",
                match.getId(), joueursInscrits);

        // Pénalité à l'organisateur
        Membre organisateur = match.getOrganisateur();
        organisateur.appliquerPenalite();
        membreRepository.save(organisateur);

        log.warn("Pénalité appliquée à l'organisateur id={} ({}). Bloqué jusqu'au {}",
                organisateur.getId(),
                organisateur.getMatricule(),
                organisateur.getPenaliteJusquA());
    }



    // --- Solde organisateur (match public incomplet) ---

    private void etape3_calculerSoldeOrganisateur(Match match) {
        /*
         * On compte les joueurs confirmés depuis la BASE pour avoir
         * le compte exact après les paiements et après l'étape 1.
         */
        long joueursConfirmes = participationRepository
                .findActivesForMatch(match.getId())
                .stream()
                .filter(p -> p.getStatut() == StatutParticipation.CONFIRME)
                .count();

        if (joueursConfirmes >= 4) {
            log.info("Match public id={} complet ({} joueurs), pas de solde",
                    match.getId(), joueursConfirmes);
            return;
        }

        long placesVides = 4 - joueursConfirmes;
        BigDecimal montantParJoueur = BigDecimal.valueOf(
                appProperties.getReservation().getMontantParJoueur());
        BigDecimal solde = montantParJoueur.multiply(BigDecimal.valueOf(placesVides));

        Membre organisateur = match.getOrganisateur();
        organisateur.ajouterSoldeImpaye(solde);
        membreRepository.save(organisateur);

        log.warn(
                "Match public id={} incomplet : {} place(s) vide(s). "
                        + "Solde de {}€ ajouté à l'organisateur id={}",
                match.getId(), placesVides, solde, organisateur.getId()
        );
    }


    // --- Confirmation du match si complet ---

    private void etape4_confirmerMatchComplet(Match match) {
        /*
         * On recompte depuis la BASE pour avoir l'état final exact.
         */
        long joueursConfirmes = participationRepository
                .findActivesForMatch(match.getId())
                .stream()
                .filter(p -> p.getStatut() == StatutParticipation.CONFIRME)
                .count();

        if (joueursConfirmes >= 4) {
            match.setStatut(StatutMatch.CONFIRME);
            matchRepository.save(match);
            log.info("Match id={} confirmé ({} joueurs payés)", match.getId(), joueursConfirmes);
        } else {
            log.info("Match id={} reste EN_ATTENTE ({} joueur(s) confirmé(s))",
                    match.getId(), joueursConfirmes);
        }
    }


    // --- Helper - rechargement depuis la base ---
    /**
     * Recharge le match depuis la base de données avec ses participations.
     * Nécessaire après chaque étape qui modifie les participations,
     * pour contourner le cache JPA de premier niveau.
     */
    private Match rechargerMatch(Long matchId) {
        return matchRepository.findByIdWithParticipations(matchId)
                .orElseThrow(() -> new IllegalStateException(
                        "Match introuvable après rechargement : id=" + matchId));
    }


}
