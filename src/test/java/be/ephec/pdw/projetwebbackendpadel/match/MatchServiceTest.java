package be.ephec.pdw.projetwebbackendpadel.match;

import be.ephec.pdw.projetwebbackendpadel.dto.matchDto.MatchResponse;
import be.ephec.pdw.projetwebbackendpadel.entity.Match;
import be.ephec.pdw.projetwebbackendpadel.exception.ResourceNotFoundException;
import be.ephec.pdw.projetwebbackendpadel.mapper.MatchMapper;
import be.ephec.pdw.projetwebbackendpadel.repository.MatchRepository;
import be.ephec.pdw.projetwebbackendpadel.service.MatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MatchServiceTest {

    // On crée de "faux" objets (Mocks) pour les dépendances
    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchMapper matchMapper;

    // On injecte les Mocks dans notre vrai service à tester
    @InjectMocks
    private MatchService matchService;

    @Test
    void findById_DoitRetournerUnMatchResponse_QuandLeMatchExiste() {
        // 1. Préparation (Arrange)
        Long matchId = 1L;
        Match mockMatch = new Match();
        mockMatch.setId(matchId);

        MatchResponse mockResponse = new MatchResponse();
        mockResponse.setId(matchId);

        // On dit au faux Repository quoi répondre
        when(matchRepository.findByIdWithParticipations(matchId)).thenReturn(Optional.of(mockMatch));
        when(matchMapper.toResponse(mockMatch)).thenReturn(mockResponse);

        // 2. Action (Act)
        MatchResponse resultat = matchService.findById(matchId);

        // 3. Vérification (Assert)
        assertNotNull(resultat);
        assertEquals(1L, resultat.getId());
        verify(matchRepository, times(1)).findByIdWithParticipations(matchId); // Vérifie que la méthode a bien été appelée
    }

    @Test
    void findById_DoitLancerException_QuandLeMatchNExistePas() {
        // 1. Préparation
        Long matchId = 99L;
        when(matchRepository.findByIdWithParticipations(matchId)).thenReturn(Optional.empty());

        // 2 & 3. Action et Vérification
        assertThrows(ResourceNotFoundException.class, () -> {
            matchService.findById(matchId);
        });
    }

}
