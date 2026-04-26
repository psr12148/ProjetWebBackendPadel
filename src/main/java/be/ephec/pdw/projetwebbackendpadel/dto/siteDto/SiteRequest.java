package be.ephec.pdw.projetwebbackendpadel.dto.siteDto;


import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteRequest {

    @NotBlank(message = "Le nom du site est obligatoire")
    @Size(max = 100)
    private String nom;

    @NotBlank(message = "L'adresse est obligatoire")
    @Size(max = 255)
    private String adresse;

    @Min(value = 1, message = "Un site doit avoir au moins 1 terrain")
    @Max(50)
    private int nbTerrains;

    @NotNull(message = "L'heure d'ouverture est obligatoire")
    private LocalTime heureOuverture;

    @NotNull(message = "L'heure de fermeture est obligatoire")
    private LocalTime heureFermeture;

    @Min(2024)
    private int anneeApplicable;
}
