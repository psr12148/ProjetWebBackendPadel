package be.ephec.pdw.projetwebbackendpadel.dto.membreDto;

import be.ephec.pdw.projetwebbackendpadel.enums.TypeMembre;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MembreResponse {

    private Long id;
    private String matricule;
    private TypeMembre typeMembre;
    private Long siteId;
    private String siteNom;
    private String nom;
    private String prenom;
    private String email;
    private BigDecimal soldeImpaye;
    private LocalDate penaliteJusquA;
    private boolean penaliteActive;         // calculé
    private boolean soldeImpayes;           // calculé
    private String typeLabel;               // "Membre Global", "Membre Site", "Membre Libre"
}
