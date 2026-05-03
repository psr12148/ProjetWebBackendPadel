package be.ephec.pdw.projetwebbackendpadel.dto.authDto;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AuthResponse {

    private String token;
    private String type = "Bearer";
    private Long   membreId;
    private String matricule;
    private String email;
    private String nom;
    private String prenom;
    private String typeLabel;
    private boolean admin;

}
