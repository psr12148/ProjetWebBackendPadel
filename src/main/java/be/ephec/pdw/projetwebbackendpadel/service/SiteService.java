package be.ephec.pdw.projetwebbackendpadel.service;

import be.ephec.pdw.projetwebbackendpadel.config.AppProperties;
import be.ephec.pdw.projetwebbackendpadel.dto.siteDto.SiteRequest;
import be.ephec.pdw.projetwebbackendpadel.dto.siteDto.SiteResponse;
import be.ephec.pdw.projetwebbackendpadel.entity.Site;
import be.ephec.pdw.projetwebbackendpadel.exception.BusinessException;
import be.ephec.pdw.projetwebbackendpadel.exception.ConflictException;
import be.ephec.pdw.projetwebbackendpadel.exception.ResourceNotFoundException;
import be.ephec.pdw.projetwebbackendpadel.mapper.SiteMapper;
import be.ephec.pdw.projetwebbackendpadel.repository.SiteRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j                              // Permet d'ajouter un logger à la classe
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)     // Par défaut, toutes les méthodes sont en lecture seule
public class SiteService {

    private  final SiteRepository siteRepository;
    private  final SiteMapper siteMapper;
    private final AppProperties appProperties;

    // Lecture

    public List<SiteResponse> findAll() {
        return siteRepository.findAll()
                .stream()
                .map(siteMapper::toResponse)
                .toList();
    }

    public SiteResponse findById(Long id) {
        return siteMapper.toResponse(getSiteOrThrow(id));
    }

    public List<SiteResponse> findSitesOuverts(LocalDate date) {
        return siteRepository.findSitesOuverts(date, date.getYear())
                .stream()
                .map(siteMapper::toResponse)
                .toList();
    }

    public List<SiteResponse> search(String query) {
        return siteRepository.search(query)
                .stream()
                .map(siteMapper::toResponse)
                .toList();
    }


    // Ecriture

    @Transactional
    public SiteResponse create(SiteRequest request) {
        validerHoraires(request);
        validerNomUnique(request.getNom(), null);

        Site site = siteMapper.toEntity(request);
        Site saved = siteRepository.save(site);

        log.info("Site créé : id={}, nom={}", saved.getId(), saved.getNom());
        return siteMapper.toResponse(saved);
    }

    @Transactional
    public SiteResponse update(Long id, SiteRequest request) {
        Site site = getSiteOrThrow(id);

        validerHoraires(request);
        validerNomUnique(request.getNom(), id);

        siteMapper.updateEntity(request, site);
        Site saved = siteRepository.save(site);

        log.info("Site mis à jour : id={}, nom={}", saved.getId(), saved.getNom());
        return siteMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Site site = getSiteOrThrow(id);

        // Vérification : on ne peut pas supprimer un site qui a des matchs à venir
        boolean aDesMatchsAVenir = !site.getTerrains().isEmpty() &&
                site.getTerrains().stream().anyMatch(t -> !t.getMatchs().isEmpty());

        if (aDesMatchsAVenir) {
            throw new BusinessException(
                    "Impossible de supprimer le site : des matchs sont planifiés sur ses terrains."
            );
        }

        log.warn("Suppression du site : id={}, nom={}", site.getId(), site.getNom());
        siteRepository.delete(site);
    }


    // Helpers privés

    public Site getSiteOrThrow(Long id) {
        return siteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException( "Site introuvable : id=" + id));
    }

    private void validerHoraires(SiteRequest request) {
        if (!request.getHeureOuverture().isBefore(request.getHeureFermeture())) {
            throw new BusinessException(
                    "L'heure d'ouverture doit être avant l'heure de fermeture.");
        }

        int dureeMinutes = appProperties.getReservation().getDureeMatchMinutes()
                + appProperties.getReservation().getPauseEntreMatchsMinutes();

        long minutesDisponibles = java.time.Duration
                .between(request.getHeureOuverture(), request.getHeureFermeture())
                .toMinutes();

        if (minutesDisponibles < dureeMinutes) {
            throw new BusinessException(
                    String.format(
                            "La plage horaire doit permettre au moins 1 match complet (durée totale : %d minutes)",
                            dureeMinutes
                    )
            );
        }
    }

    private void validerNomUnique(String nom, Long idExistant) {
        boolean doublon = (idExistant == null)
                ? siteRepository.existsByNom(nom)
                : siteRepository.existsByNomAndIdNot(nom, idExistant);

        if (doublon) {
            throw new ConflictException("Un site avec ce nom existe déjà : " + nom);
        }
    }



}
