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

        // ÉTAPE 1 : Libérer les places non payées
        etape1_libererPlacesNonPayees(match);

        // ÉTAPE 2 : Bascule privé → public si incomplet
        if (match.getTypeMatch() == TypeMatch.PRIVE) {
            etape2_basculerPriveEnPublicSiIncomplet(match);
        }

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
            // Ne pas libérer l'organisateur d'un match privé ici
            // (géré dans l'étape 2 avec pénalité)
            if (match.getTypeMatch() == TypeMatch.PRIVE
                    && p.getMembre().getId().equals(match.getOrganisateur().getId())) {
                continue;
            }

            p.liberer();
            participationRepository.save(p);

            log.info("Place libérée : match id={}, membre id={} (non payé)",
                    match.getId(), p.getMembre().getId());
        }
    }



    // --- Bascule privé -> public + pénalité organisateur ---

    private void etape2_basculerPriveEnPublicSiIncomplet(Match match) {
        long joueursActifs = match.getParticipations().stream()
                .filter(p -> p.getStatut() != StatutParticipation.LIBERE)
                .count();

        if (joueursActifs >= 4) {
            log.info("Match privé id={} : complet ({} joueurs), pas de bascule",
                    match.getId(), joueursActifs);
            return;
        }

        // Bascule en public
        match.setTypeMatch(TypeMatch.PUBLIC);
        matchRepository.save(match);

        log.warn("Match id={} basculé de PRIVE à PUBLIC ({} joueur(s) actif(s))",
                match.getId(), joueursActifs);

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
        long joueursConfirmes = match.getParticipations().stream()
                .filter(p -> p.getStatut() == StatutParticipation.CONFIRME)
                .count();

        if (joueursConfirmes >= 4) return;

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
        long joueursConfirmes = match.getParticipations().stream()
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


}
