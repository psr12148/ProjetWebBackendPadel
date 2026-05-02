package be.ephec.pdw.projetwebbackendpadel.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DashboardStats {

    // --- Chiffres globaux ---
    private long    totalSites;
    private long    totalTerrains;
    private long    totalMembres;
    private long    totalMatchsAujourdhui;
    private long    totalMatchsSemaine;

    // --- Matchs ---
    private long    matchsEnAttente;
    private long    matchsConfirmes;
    private long    matchsPublicsDisponibles;
    private long    matchsPrivesIncomplets;   // à risque de bascule

    // --- Paiements ---
    private BigDecimal chiffreAffairesJour;
    private BigDecimal chiffreAffairesSemaine;
    private long       membresAvecSoldeImpaye;
    private BigDecimal totalSoldesImpayes;

    // --- Alertes J-1 ---
    private long    alertesMatchsPrivesIncomplets;  // basculeront demain
    private long    alertesPlacesNonPayees;         // seront libérées demain

    // --- Taux d'occupation par site ---
    private List<TauxOccupationSite> tauxOccupationParSite;

    // --- Membres avec pénalité active ---
    private long    membresAvecPenalite;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TauxOccupationSite {
        private Long   siteId;
        private String siteNom;
        private long   creneauxTotal;
        private long   creneauxOccupes;
        private double tauxOccupation;  // 0.0 à 100.0
    }

}
