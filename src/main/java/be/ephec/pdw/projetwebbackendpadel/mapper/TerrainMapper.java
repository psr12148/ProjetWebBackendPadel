package be.ephec.pdw.projetwebbackendpadel.mapper;

import be.ephec.pdw.projetwebbackendpadel.dto.terrainDto.TerrainRequest;
import be.ephec.pdw.projetwebbackendpadel.dto.terrainDto.TerrainResponse;
import be.ephec.pdw.projetwebbackendpadel.entity.Terrain;
import org.mapstruct.*;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TerrainMapper {

    @Mapping(target = "siteId",       source = "site.id")
    @Mapping(target = "siteNom",      source = "site.nom")
    @Mapping(target = "nomAffichage", ignore = true)
    TerrainResponse toResponse(Terrain terrain);

    @AfterMapping
    default void setNomAffichage(Terrain terrain, @MappingTarget TerrainResponse response) {
        response.setNomAffichage(terrain.getNomAffichage());
    }

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "site",      ignore = true)   // résolu dans le service via siteId
    @Mapping(target = "matchs",    ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Terrain toEntity(TerrainRequest request);

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "site",      ignore = true)
    @Mapping(target = "matchs",    ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateEntity(TerrainRequest request, @MappingTarget Terrain terrain);
}
