package be.ephec.pdw.projetwebbackendpadel.service;

import be.ephec.pdw.projetwebbackendpadel.dto.DashboardStats;
import be.ephec.pdw.projetwebbackendpadel.entity.Site;
import be.ephec.pdw.projetwebbackendpadel.enums.StatutMatch;
import be.ephec.pdw.projetwebbackendpadel.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final SiteRepository      siteRepository;
    private final TerrainRepository   terrainRepository;
    private final MembreRepository    membreRepository;

    public DashboardStats getStats() {
        LocalDate today     = LocalDate.now();
        LocalDate      demain    = today.plusDays(1);
        LocalDateTime  debutJour = today.atStartOfDay();
        LocalDateTime  finJour   = today.atTime(LocalTime.MAX);
        LocalDateTime  debutSem  = today.with(java.time.DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime  finSem    = debutSem.plusWeeks(1);
        LocalDateTime  now       = LocalDateTime.now();

        // --- Chiffres de base ---
        long totalSites    = siteRepository.count();
        long totalTerrains = terrainRepository.count();
        long totalMembres  = membreRepository.count();

        // --- Matchs ---
        long matchsAujourdhui = dashboardRepository.countMatchsBetween(debutJour, finJour);
        long matchsSemaine    = dashboardRepository.countMatchsBetween(debutSem,  finSem);
        long matchsEnAttente  = dashboardRepository.countMatchsByStatutBetween(
                StatutMatch.EN_ATTENTE, debutSem, finSem);
        long matchsConfirmes  = dashboardRepository.countMatchsByStatutBetween(
                StatutMatch.CONFIRME, debutSem, finSem);
        long matchsPublics    = dashboardRepository.countMatchsPublicsDisponibles(now);
        long matchsPrivesInc  = dashboardRepository.countMatchsPrivesIncomplets(now);

        // --- Paiements ---
        var caJour    = dashboardRepository.chiffreAffairesBetween(debutJour, finJour);
        var caSemaine = dashboardRepository.chiffreAffairesBetween(debutSem,  finSem);
        long membresAvecSolde  = dashboardRepository.countMembresAvecSoldeImpaye();
        var  totalSoldes       = dashboardRepository.totalSoldesImpayes();
        long membresAvecPenal  = dashboardRepository.countMembresAvecPenalite(today);

        // --- Alertes J-1 ---
        long alertesPrives   = dashboardRepository.countAlertesMatchsPrivesDemain(demain);
        long alertesNonPayes = dashboardRepository.countAlertesPlacesNonPayeesDemain(demain);

        // --- Taux d'occupation par site (sur la semaine) ---
        List<DashboardStats.TauxOccupationSite> tauxParSite =
                calculerTauxOccupation(debutSem, finSem);

        return DashboardStats.builder()
                .totalSites(totalSites)
                .totalTerrains(totalTerrains)
                .totalMembres(totalMembres)
                .totalMatchsAujourdhui(matchsAujourdhui)
                .totalMatchsSemaine(matchsSemaine)
                .matchsEnAttente(matchsEnAttente)
                .matchsConfirmes(matchsConfirmes)
                .matchsPublicsDisponibles(matchsPublics)
                .matchsPrivesIncomplets(matchsPrivesInc)
                .chiffreAffairesJour(caJour)
                .chiffreAffairesSemaine(caSemaine)
                .membresAvecSoldeImpaye(membresAvecSolde)
                .totalSoldesImpayes(totalSoldes)
                .membresAvecPenalite(membresAvecPenal)
                .alertesMatchsPrivesIncomplets(alertesPrives)
                .alertesPlacesNonPayees(alertesNonPayes)
                .tauxOccupationParSite(tauxParSite)
                .build();

    }


    // --- Helpers ---

    private List<DashboardStats.TauxOccupationSite> calculerTauxOccupation(
            LocalDateTime debut, LocalDateTime fin) {

        List<Site> sites = siteRepository.findAll();

        // Nombre de créneaux occupés par siteId
        Map<Long, Long> occupesParSite = dashboardRepository
                .countCreneauxOccupesParSite(debut, fin)
                .stream()
                .collect(Collectors.toMap(
                        r -> (Long) r[0],
                        r -> (Long) r[1]
                ));

        List<DashboardStats.TauxOccupationSite> result = new ArrayList<>();

        for (Site site : sites) {
            long nbTerrains  = terrainRepository.countBySiteId(site.getId());
            long creneauxTotal  = nbTerrains * site.nombreCreneauxParJour() * 7; // 7 jours
            long creneauxOccupes = occupesParSite.getOrDefault(site.getId(), 0L);
            double taux = creneauxTotal > 0
                    ? (double) creneauxOccupes / creneauxTotal * 100.0
                    : 0.0;

            result.add(DashboardStats.TauxOccupationSite.builder()
                    .siteId(site.getId())
                    .siteNom(site.getNom())
                    .creneauxTotal(creneauxTotal)
                    .creneauxOccupes(creneauxOccupes)
                    .tauxOccupation(Math.round(taux * 10.0) / 10.0)
                    .build());
        }

        return result;
    }
}
