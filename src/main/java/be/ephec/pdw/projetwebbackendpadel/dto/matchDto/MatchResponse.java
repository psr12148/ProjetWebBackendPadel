package be.ephec.pdw.projetwebbackendpadel.dto.matchDto;

import be.ephec.pdw.projetwebbackendpadel.dto.participationDto.ParticipationResponse;
import be.ephec.pdw.projetwebbackendpadel.enums.StatutMatch;
import be.ephec.pdw.projetwebbackendpadel.enums.TypeMatch;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MatchResponse {

    private Long          id;
    private Long          terrainId;
    private String        terrainNom;
    private Long          siteId;
    private String        siteNom;
    private Long          organisateurId;
    private String        organisateurNom;
    private LocalDateTime dateHeure;
    private TypeMatch typeMatch;
    private StatutMatch statut;
    private BigDecimal montantTotal;
    private long          nombreJoueursConfirmes;
    private long          placesDisponibles;
    private boolean       complet;
    private List<ParticipationResponse> participations;
}
