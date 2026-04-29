package be.ephec.pdw.projetwebbackendpadel.terrain;

import be.ephec.pdw.projetwebbackendpadel.dto.terrainDto.TerrainRequest;
import be.ephec.pdw.projetwebbackendpadel.dto.terrainDto.TerrainResponse;
import be.ephec.pdw.projetwebbackendpadel.entity.Match;
import be.ephec.pdw.projetwebbackendpadel.entity.Site;
import be.ephec.pdw.projetwebbackendpadel.entity.Terrain;
import be.ephec.pdw.projetwebbackendpadel.exception.BusinessException;
import be.ephec.pdw.projetwebbackendpadel.exception.ConflictException;
import be.ephec.pdw.projetwebbackendpadel.exception.ResourceNotFoundException;
import be.ephec.pdw.projetwebbackendpadel.mapper.TerrainMapper;
import be.ephec.pdw.projetwebbackendpadel.repository.TerrainRepository;
import be.ephec.pdw.projetwebbackendpadel.service.SiteService;
import be.ephec.pdw.projetwebbackendpadel.service.TerrainService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TerrainServiceTest {
    // 1. On "Mock" (simule) les dépendances du Service
    @Mock
    private TerrainRepository terrainRepository;

    @Mock
    private TerrainMapper terrainMapper;

    @Mock
    private SiteService siteService;

    // 2. On injecte ces faux objets dans le vrai TerrainService
    @InjectMocks
    private TerrainService terrainService;

    // --- TEST 1 : Cas de succès (Trouver un terrain) ---
    @Test
    void findById_DoitRetournerUnTerrain_QuandIlExiste() {
        // Arrange (Préparation)
        Long terrainId = 1L;
        Terrain fauxTerrain = new Terrain();
        fauxTerrain.setId(terrainId);

        TerrainResponse fausseResponse = new TerrainResponse();
        fausseResponse.setId(terrainId);

        // On dicte le comportement de nos mocks
        when(terrainRepository.findById(terrainId)).thenReturn(Optional.of(fauxTerrain));
        when(terrainMapper.toResponse(fauxTerrain)).thenReturn(fausseResponse);

        // Act (Action)
        TerrainResponse resultat = terrainService.findById(terrainId);

        // Assert (Vérification)
        assertNotNull(resultat);
        assertEquals(1L, resultat.getId());

        // On vérifie que le repository a bien été appelé exactement 1 fois
        verify(terrainRepository, times(1)).findById(terrainId);
    }

    // --- TEST 2 : Cas d'erreur (Terrain introuvable) ---
    @Test
    void findById_DoitLancerException_QuandTerrainNExistePas() {
        // Arrange
        Long terrainId = 99L;
        when(terrainRepository.findById(terrainId)).thenReturn(Optional.empty());

        // Act & Assert
        // On vérifie que l'appel de la méthode lance bien une ResourceNotFoundException
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            terrainService.findById(terrainId);
        });

        assertTrue(exception.getMessage().contains("Terrain introuvable"));

        // On vérifie que le mapper n'a jamais été appelé (puisque ça a planté avant)
        verify(terrainMapper, never()).toResponse(any());
    }

    // --- TEST 3 : Règle métier (Suppression impossible si matchs prévus) ---
    @Test
    void delete_DoitLancerException_QuandTerrainAPrevuDesMatchs() {
        // Arrange
        Long terrainId = 1L;
        Terrain fauxTerrain = new Terrain();
        fauxTerrain.setId(terrainId);

        // On simule qu'il y a un match sur ce terrain
        fauxTerrain.setMatchs(List.of(new Match()));

        when(terrainRepository.findById(terrainId)).thenReturn(Optional.of(fauxTerrain));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            terrainService.delete(terrainId);
        });

        assertEquals("Impossible de supprimer le terrain : des matchs y sont planifiés.", exception.getMessage());

        // On vérifie que le delete de la base de données n'a JAMAIS été appelé par sécurité
        verify(terrainRepository, never()).delete(any());
    }

    // --- TEST 4 : CREATE - Cas de succès ---
    @Test
    void create_DoitCreerTerrain_QuandValidationsPassent() {
        // Arrange
        TerrainRequest request = new TerrainRequest();
        request.setSiteId(1L);
        request.setNumero(3);

        Site fauxSite = new Site();
        fauxSite.setId(1L);
        fauxSite.setNom("Padel Club");
        fauxSite.setNbTerrains(4); // Capacité max de 4 terrains

        Terrain fauxTerrain = new Terrain();
        Terrain terrainSauvegarde = new Terrain();
        terrainSauvegarde.setId(10L);
        terrainSauvegarde.setNumero(3);

        TerrainResponse fausseResponse = new TerrainResponse();
        fausseResponse.setId(10L);

        // 1. Le site existe
        when(siteService.getSiteOrThrow(1L)).thenReturn(fauxSite);
        // 2. Il y a actuellement 2 terrains (donc < 4, la capacité est bonne)
        when(terrainRepository.countBySiteId(1L)).thenReturn(2L);
        // 3. Le numéro 3 n'est pas encore pris
        when(terrainRepository.existsByNumeroOnSite(1L, 3)).thenReturn(false);

        when(terrainMapper.toEntity(request)).thenReturn(fauxTerrain);
        when(terrainRepository.save(fauxTerrain)).thenReturn(terrainSauvegarde);
        when(terrainMapper.toResponse(terrainSauvegarde)).thenReturn(fausseResponse);

        // Act
        TerrainResponse resultat = terrainService.create(request);

        // Assert
        assertNotNull(resultat);
        assertEquals(10L, resultat.getId());
        assertEquals(fauxSite, fauxTerrain.getSite()); // On vérifie que le site a bien été attaché
        verify(terrainRepository, times(1)).save(any(Terrain.class));
    }

    // --- TEST 5 : CREATE - Erreur (Capacité maximale atteinte) ---
    @Test
    void create_DoitLancerException_QuandCapaciteSiteAtteinte() {
        // Arrange
        TerrainRequest request = new TerrainRequest();
        request.setSiteId(1L);

        Site fauxSite = new Site();
        fauxSite.setId(1L);
        fauxSite.setNbTerrains(4); // Capacité max de 4

        when(siteService.getSiteOrThrow(1L)).thenReturn(fauxSite);
        // On simule qu'il y a DEJA 4 terrains dans la base de données
        when(terrainRepository.countBySiteId(1L)).thenReturn(4L);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            terrainService.create(request);
        });

        assertTrue(exception.getMessage().contains("a déjà atteint sa capacité maximale"));

        // Sécurité : On vérifie que le save n'a JAMAIS été appelé !
        verify(terrainRepository, never()).save(any());
    }

    // --- TEST 6 : UPDATE - Cas de succès ---
    @Test
    void update_DoitMettreAJourTerrain_QuandValidationsPassent() {
        // Arrange
        Long terrainId = 10L;
        TerrainRequest request = new TerrainRequest();
        request.setSiteId(1L);
        request.setNumero(5);

        Terrain terrainExistant = new Terrain();
        terrainExistant.setId(terrainId);

        Site fauxSite = new Site();
        fauxSite.setId(1L);
        fauxSite.setNbTerrains(6);

        TerrainResponse fausseResponse = new TerrainResponse();
        fausseResponse.setId(terrainId);

        when(terrainRepository.findById(terrainId)).thenReturn(Optional.of(terrainExistant));
        when(siteService.getSiteOrThrow(1L)).thenReturn(fauxSite);

        // On vérifie les AUTRES terrains (excludeId)
        when(terrainRepository.countBySiteIdAndIdNot(1L, terrainId)).thenReturn(3L);
        when(terrainRepository.existsByNumeroOnSiteExcluding(1L, 5, terrainId)).thenReturn(false);

        // Attention: updateEntity ne retourne rien (void), pas besoin de "when" strict
        when(terrainRepository.save(terrainExistant)).thenReturn(terrainExistant);
        when(terrainMapper.toResponse(terrainExistant)).thenReturn(fausseResponse);

        // Act
        TerrainResponse resultat = terrainService.update(terrainId, request);

        // Assert
        assertNotNull(resultat);
        verify(terrainMapper, times(1)).updateEntity(request, terrainExistant);
        verify(terrainRepository, times(1)).save(terrainExistant);
    }

    // --- TEST 7 : UPDATE - Erreur (Numéro en doublon) ---
    @Test
    void update_DoitLancerException_QuandNumeroDejaPris() {
        // Arrange
        Long terrainId = 10L;
        TerrainRequest request = new TerrainRequest();
        request.setSiteId(1L);
        request.setNumero(2); // On essaie de lui donner le N°2

        Terrain terrainExistant = new Terrain();
        terrainExistant.setId(terrainId);

        Site fauxSite = new Site();
        fauxSite.setId(1L);
        fauxSite.setNbTerrains(10); // Beaucoup de place

        when(terrainRepository.findById(terrainId)).thenReturn(Optional.of(terrainExistant));
        when(siteService.getSiteOrThrow(1L)).thenReturn(fauxSite);
        when(terrainRepository.countBySiteIdAndIdNot(1L, terrainId)).thenReturn(5L); // Capacité OK

        // BOUM : Le N°2 est DÉJÀ pris par un autre terrain !
        when(terrainRepository.existsByNumeroOnSiteExcluding(1L, 2, terrainId)).thenReturn(true);

        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class, () -> {
            terrainService.update(terrainId, request);
        });

        assertTrue(exception.getMessage().contains("existe déjà sur ce site"));
        verify(terrainRepository, never()).save(any());
    }

}
