package be.ephec.pdw.projetwebbackendpadel.dto.authDto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LoginRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String motDePasse;
}
