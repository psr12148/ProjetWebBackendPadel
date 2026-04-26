package be.ephec.pdw.projetwebbackendpadel.dto.terrainDto;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TerrainResponse {

    private Long id;
    private Long siteId;
    private String siteNom;         // dénormalisé pour éviter un appel supplémentaire côté Angular
    private int numero;
    private String nom;
    private String nomAffichage;

}
