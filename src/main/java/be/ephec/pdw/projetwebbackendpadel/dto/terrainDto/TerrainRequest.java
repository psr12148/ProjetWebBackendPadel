package be.ephec.pdw.projetwebbackendpadel.dto.terrainDto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TerrainRequest {

    @NotNull(message = "Le siteId est obligatoire")
    private Long siteId;

    @Min(value = 1, message = "Le numéro de terrain doit être >= 1")
    private int numero;

    @Size(max = 50)
    private String nom;
}
