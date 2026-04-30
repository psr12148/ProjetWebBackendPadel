package be.ephec.pdw.projetwebbackendpadel.dto.participationDto;

import be.ephec.pdw.projetwebbackendpadel.enums.StatutParticipation;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ParticipationResponse {

    private Long               id;
    private Long               membreId;
    private String             membreNom;
    private String             membreMatricule;
    private StatutParticipation statut;
    private BigDecimal montantDu;
    private BigDecimal         montantPaye;
    private boolean            payee;
}
