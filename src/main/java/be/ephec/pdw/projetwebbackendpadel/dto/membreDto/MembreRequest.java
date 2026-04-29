package be.ephec.pdw.projetwebbackendpadel.dto.membreDto;

import be.ephec.pdw.projetwebbackendpadel.enums.TypeMembre;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MembreRequest {

    @NotBlank
    @Pattern(
            regexp = "^[GSL]\\d{4,6}$",
            message = "Matricule invalide. Format : G0001, S00001 ou L00001"
    )
    private String matricule;

    @NotNull
    private TypeMembre typeMembre;

    // Obligatoire uniquement si typeMembre = SITE
    private Long siteId;

    @NotBlank
    @Size(max = 100)
    private String nom;

    @NotBlank
    @Size(max = 100)
    private String prenom;

    @NotBlank
    @Email
    private String email;

    // Mot de passe en clair — hashé dans le service
    @NotBlank
    @Size(min = 8)
    private String motDePasse;
}
