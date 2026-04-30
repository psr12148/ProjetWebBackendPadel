package be.ephec.pdw.projetwebbackendpadel.dto.matchDto;

import be.ephec.pdw.projetwebbackendpadel.enums.TypeMatch;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MatchRequest {

    @NotNull(message = "Le terrain est obligatoire")
    private Long terrainId;

    @NotNull(message = "La date et l'heure sont obligatoires")
    //@Future(message = "Le match doit être dans le futur")
    private LocalDateTime dateHeure;

    @NotNull(message = "Le type de match est obligatoire")
    private TypeMatch typeMatch;
}
