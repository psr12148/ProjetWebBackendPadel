package be.ephec.pdw.projetwebbackendpadel.dto.participationDto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ParticipationRequest {

    @NotNull
    private Long membreId;
}
