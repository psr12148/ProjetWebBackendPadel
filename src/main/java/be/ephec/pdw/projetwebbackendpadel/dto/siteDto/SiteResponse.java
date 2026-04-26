package be.ephec.pdw.projetwebbackendpadel.dto.siteDto;

import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteResponse {

    private Long id;
    private String nom;
    private String adresse;
    private int nbTerrains;
    private LocalTime heureOuverture;
    private LocalTime heureFermeture;
    private int anneeApplicable;

    /**
     * Champ calculé — alimenté par @AfterMapping dans SiteMapper.
     */
    private int nombreCreneauxParJour;
}
