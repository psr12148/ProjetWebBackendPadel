package be.ephec.pdw.projetwebbackendpadel.mapper;

import be.ephec.pdw.projetwebbackendpadel.dto.siteDto.SiteRequest;
import be.ephec.pdw.projetwebbackendpadel.dto.siteDto.SiteResponse;
import be.ephec.pdw.projetwebbackendpadel.entity.Site;
import org.mapstruct.*;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface SiteMapper {

    @Mapping(target = "nombreCreneauxParJour", ignore = true)
    SiteResponse toResponse(Site site);

    @AfterMapping
    default void setChampsCalcules(Site site, @MappingTarget SiteResponse response) {
        response.setNombreCreneauxParJour(site.nombreCreneauxParJour());
    }

    @Mapping(target = "id",             ignore = true)
    @Mapping(target = "terrains",       ignore = true)
    @Mapping(target = "joursFermeture", ignore = true)
    @Mapping(target = "membres",        ignore = true)
    @Mapping(target = "createdAt",      ignore = true)
    @Mapping(target = "updatedAt",      ignore = true)
    @Mapping(target = "createdBy",      ignore = true)
    @Mapping(target = "updatedBy",      ignore = true)
    Site toEntity(SiteRequest request);

    @Mapping(target = "id",             ignore = true)
    @Mapping(target = "terrains",       ignore = true)
    @Mapping(target = "joursFermeture", ignore = true)
    @Mapping(target = "membres",        ignore = true)
    @Mapping(target = "createdAt",      ignore = true)
    @Mapping(target = "updatedAt",      ignore = true)
    @Mapping(target = "createdBy",      ignore = true)
    @Mapping(target = "updatedBy",      ignore = true)
    void updateEntity(SiteRequest request, @MappingTarget Site site);
}
