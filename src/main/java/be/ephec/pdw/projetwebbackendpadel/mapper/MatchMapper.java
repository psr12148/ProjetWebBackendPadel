package be.ephec.pdw.projetwebbackendpadel.mapper;

import be.ephec.pdw.projetwebbackendpadel.dto.matchDto.MatchResponse;
import be.ephec.pdw.projetwebbackendpadel.dto.participationDto.ParticipationResponse;
import be.ephec.pdw.projetwebbackendpadel.entity.Match;
import be.ephec.pdw.projetwebbackendpadel.entity.Participation;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MatchMapper {

    @Mapping(target = "terrainId",              source = "terrain.id")
    @Mapping(target = "terrainNom",             source = "terrain.nom")
    @Mapping(target = "siteId",                 source = "terrain.site.id")
    @Mapping(target = "siteNom",                source = "terrain.site.nom")
    @Mapping(target = "organisateurId",         source = "organisateur.id")
    @Mapping(target = "organisateurNom",        expression = "java(match.getOrganisateur().getPrenom() + \" \" + match.getOrganisateur().getNom())")
    @Mapping(target = "nombreJoueursConfirmes", expression = "java(match.nombreJoueursConfirmes())")
    @Mapping(target = "placesDisponibles",      expression = "java(match.placesDisponibles())")
    @Mapping(target = "complet",                expression = "java(match.estComplet())")
    MatchResponse toResponse(Match match);

    @Mapping(target = "membreId",        source = "membre.id")
    @Mapping(target = "membreNom",       expression = "java(p.getMembre().getPrenom() + \" \" + p.getMembre().getNom())")
    @Mapping(target = "membreMatricule", source = "membre.matricule")
    @Mapping(target = "payee",           expression = "java(p.estPayee())")
    ParticipationResponse toParticipationResponse(Participation p);
}
