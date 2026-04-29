package be.ephec.pdw.projetwebbackendpadel.service;

import be.ephec.pdw.projetwebbackendpadel.dto.terrainDto.TerrainRequest;
import be.ephec.pdw.projetwebbackendpadel.dto.terrainDto.TerrainResponse;
import be.ephec.pdw.projetwebbackendpadel.entity.Site;
import be.ephec.pdw.projetwebbackendpadel.entity.Terrain;
import be.ephec.pdw.projetwebbackendpadel.exception.BusinessException;
import be.ephec.pdw.projetwebbackendpadel.exception.ConflictException;
import be.ephec.pdw.projetwebbackendpadel.exception.ResourceNotFoundException;
import be.ephec.pdw.projetwebbackendpadel.mapper.TerrainMapper;
import be.ephec.pdw.projetwebbackendpadel.repository.TerrainRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TerrainService {

    private final TerrainRepository terrainRepository;
    private final TerrainMapper terrainMapper;
    private final SiteService siteService;                  // réutilise getSiteOrThrow()

    // Lecture

    public List<TerrainResponse> findBySite(Long siteId){
        siteService.getSiteOrThrow(siteId);  // vérifie que le site existe
        return terrainRepository.findBySiteId(siteId)
                .stream()
                .map(terrainMapper::toResponse)
                .toList();
    }

    public TerrainResponse findById(Long id) {return terrainMapper.toResponse(getTerrainOrThrow(id));
    }

    public List<TerrainResponse> findDisponibles(Long siteId, LocalDateTime dateHeure) {
        return terrainRepository.findDisponibles(siteId, dateHeure)
                .stream()
                .map(terrainMapper::toResponse)
                .toList();
    }


    // Ecriture

    @Transactional
    public TerrainResponse create(TerrainRequest request) {
        Site site = siteService.getSiteOrThrow(request.getSiteId());

        validerCapaciteSite(site, null);
        validerNumeroUnique(request.getSiteId(), request.getNumero(), null);

        Terrain terrain = terrainMapper.toEntity(request);
        terrain.setSite(site);

        Terrain saved = terrainRepository.save(terrain);
        log.info("Terrain créé : id={}, site={}, numéro={}",
                saved.getId(), site.getNom(), saved.getNumero());
        return terrainMapper.toResponse(saved);
    }

    @Transactional
    public TerrainResponse update(Long id, TerrainRequest request) {
        Terrain terrain = getTerrainOrThrow(id);
        Site site = siteService.getSiteOrThrow(request.getSiteId());

        validerCapaciteSite(site, id);

        validerNumeroUnique(request.getSiteId(), request.getNumero(), id);

        terrainMapper.updateEntity(request, terrain);
        terrain.setSite(site);

        Terrain saved = terrainRepository.save(terrain);
        log.info("Terrain mis à jour : id={}", saved.getId());

        return terrainMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Terrain terrain = getTerrainOrThrow(id);

        if(!terrain.getMatchs().isEmpty()) {
            throw new BusinessException(
                    "Impossible de supprimer le terrain : des matchs y sont planifiés."
            );
        }

        log.warn("Suppression terrain : id={}, site={}",
                terrain.getId(), terrain.getSite().getNom());
        terrainRepository.delete(terrain);
    }

    // Helpers

    public Terrain getTerrainOrThrow(Long id) {
        return terrainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Terrain introuvable : id=" + id));
    }

    private void validerCapaciteSite(Site site, Long excludeId) {

        long countAutresTerrains;

        if (excludeId == null) {
            // Mode Création : on compte TOUS les terrains de ce site
            countAutresTerrains = terrainRepository.countBySiteId(site.getId());
        } else {
            // Mode Mise à jour : on compte tous les terrains du site SAUF celui qu'on est en train de modifier
            countAutresTerrains = terrainRepository.countBySiteIdAndIdNot(site.getId(), excludeId);
        }

        // Si le nombre d'AUTRES terrains prend déjà toute la place, on bloque
        if (countAutresTerrains >= site.getNbTerrains()) {
            throw new BusinessException(String.format(
                    "Le site \"%s\" a déjà atteint sa capacité maximale de %d terrain(s).",
                    site.getNom(), site.getNbTerrains()
            ));
        }
    }

    private void validerNumeroUnique(Long siteId, int numero, Long excludeId) {
        boolean doublon = (excludeId == null)
                ? terrainRepository.existsByNumeroOnSite(siteId, numero)
                : terrainRepository.existsByNumeroOnSiteExcluding(siteId, numero, excludeId);

        if (doublon) {
            throw new ConflictException(
                    "Le terrain N°" + numero + " existe déjà sur ce site."
            );
        }
    }


}
