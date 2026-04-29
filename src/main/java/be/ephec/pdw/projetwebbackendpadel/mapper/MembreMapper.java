package be.ephec.pdw.projetwebbackendpadel.mapper;

import be.ephec.pdw.projetwebbackendpadel.dto.membreDto.MembreRequest;
import be.ephec.pdw.projetwebbackendpadel.dto.membreDto.MembreResponse;
import be.ephec.pdw.projetwebbackendpadel.entity.Membre;
import org.mapstruct.*;

import java.time.LocalDate;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MembreMapper {

    @Mapping(target = "siteId",        source = "site.id")
    @Mapping(target = "siteNom",       source = "site.nom")
    @Mapping(target = "penaliteActive", ignore = true)
    @Mapping(target = "soldeImpayes",   ignore = true)
    @Mapping(target = "typeLabel",      ignore = true)
    MembreResponse toResponse(Membre membre);

    @AfterMapping
    default void setChampsCalcules(Membre membre, @MappingTarget MembreResponse response) {
        response.setPenaliteActive(membre.aPenaliteActive(LocalDate.now()));
        response.setSoldeImpayes(membre.aSoldeImpaye());
        response.setTypeLabel(switch (membre.getTypeMembre()) {
            case GLOBAL -> "Membre Global";
            case SITE   -> "Membre Site";
            case LIBRE  -> "Membre Libre";
        });
    }

    @Mapping(target = "id",               ignore = true)
    @Mapping(target = "motDePasseHash",   ignore = true)  // hashé dans le service
    @Mapping(target = "site",             ignore = true)  // résolu dans le service
    @Mapping(target = "soldeImpaye",      ignore = true)  // initialisé à 0 par défaut
    @Mapping(target = "penaliteJusquA",   ignore = true)
    @Mapping(target = "matchsOrganises",  ignore = true)
    @Mapping(target = "participations",   ignore = true)
    @Mapping(target = "createdAt",        ignore = true)
    @Mapping(target = "updatedAt",        ignore = true)
    @Mapping(target = "createdBy",        ignore = true)
    @Mapping(target = "updatedBy",        ignore = true)
    Membre toEntity(MembreRequest request);

    @Mapping(target = "id",               ignore = true)
    @Mapping(target = "motDePasseHash",   ignore = true)
    @Mapping(target = "site",             ignore = true)
    @Mapping(target = "soldeImpaye",      ignore = true)
    @Mapping(target = "penaliteJusquA",   ignore = true)
    @Mapping(target = "matchsOrganises",  ignore = true)
    @Mapping(target = "participations",   ignore = true)
    @Mapping(target = "createdAt",        ignore = true)
    @Mapping(target = "updatedAt",        ignore = true)
    @Mapping(target = "createdBy",        ignore = true)
    @Mapping(target = "updatedBy",        ignore = true)
    void updateEntity(MembreRequest request, @MappingTarget Membre membre);

}
