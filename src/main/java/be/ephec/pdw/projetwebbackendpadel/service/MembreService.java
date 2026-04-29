package be.ephec.pdw.projetwebbackendpadel.service;

import be.ephec.pdw.projetwebbackendpadel.config.AppProperties;
import be.ephec.pdw.projetwebbackendpadel.dto.membreDto.MembreRequest;
import be.ephec.pdw.projetwebbackendpadel.dto.membreDto.MembreResponse;
import be.ephec.pdw.projetwebbackendpadel.entity.Membre;
import be.ephec.pdw.projetwebbackendpadel.entity.Site;
import be.ephec.pdw.projetwebbackendpadel.enums.TypeMembre;
import be.ephec.pdw.projetwebbackendpadel.exception.*;
import be.ephec.pdw.projetwebbackendpadel.mapper.MembreMapper;
import be.ephec.pdw.projetwebbackendpadel.repository.MembreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MembreService {

    private final MembreRepository membreRepository;
    private final MembreMapper membreMapper;
    private final SiteService siteService;
    private final AppProperties appProperties;
    private final PasswordEncoder passwordEncoder;

    // --- Lecture ---

    public List<MembreResponse> findAll() {
        return membreRepository.findAll()
                .stream()
                .map(membreMapper::toResponse)
                .toList();
    }

    public MembreResponse findById(Long id) {
        return membreMapper.toResponse(getMembreOrThrow(id));
    }

    public List<MembreResponse> findByType(TypeMembre type) {
        return membreRepository.findByType(type)
                .stream()
                .map(membreMapper::toResponse)
                .toList();
    }

    public List<MembreResponse> findBySite(Long siteId) {
        siteService.getSiteOrThrow(siteId); // validation d'existence du site
        return membreRepository.findBySite(siteId)
                .stream()
                .map(membreMapper::toResponse)
                .toList();
    }

    public List<MembreResponse> search(String query) {
        return membreRepository.search(query)
                .stream()
                .map(membreMapper::toResponse)
                .toList();
    }

    public List<MembreResponse> findAvecSoldeImpaye() {
        return membreRepository.findAvecSoldeImpaye()
                .stream()
                .map(membreMapper::toResponse)
                .toList();
    }


    // --- Ecriture ---

    @Transactional
    public MembreResponse create(MembreRequest request) {
        validerMaticuleCoherent(request);
        validerMatriculeUnique(request.getMatricule(), null);
        validerEmailUnique(request.getEmail(), null);

        Site site = resoudreSite(request);      //Résout le site selon le type de membre

        Membre membre = membreMapper.toEntity(request);
        membre.setSite(site);
        membre.setMotDePasseHash(passwordEncoder.encode(request.getMotDePasse()));

        Membre saved = membreRepository.save(membre);
        log.info("Membre créé : id={}, matricule={}, type={}",
                saved.getId(), saved.getMatricule(), saved.getTypeMembre());

        return membreMapper.toResponse(saved);

    }

    @Transactional
    public MembreResponse update(Long id, MembreRequest request) {
        Membre membre = getMembreOrThrow(id);

        validerMaticuleCoherent(request);
        validerMatriculeUnique(request.getMatricule(), id);
        validerEmailUnique(request.getEmail(), id);

        Site site = resoudreSite(request);      //Résout le site selon le type de membre

        membreMapper.updateEntity(request, membre);
        membre.setSite(site);

        // Ne re-hashe que si un nouveau mot de passe est fourni
        if (request.getMotDePasse() != null && !request.getMotDePasse().isBlank()) {
            membre.setMotDePasseHash(passwordEncoder.encode(request.getMotDePasse()));
        }

        Membre saved = membreRepository.save(membre);
        log.info("Membre mis à jour : id={}", saved.getId());

        return membreMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Membre membre = getMembreOrThrow(id);

        if (!membre.getParticipations().isEmpty()) {
            throw new BusinessException(
                    "Impossible de supprimer ce membre : il a des participations enregistrées."
            );
        }

        log.warn("Suppression membre : id={}, matricule={}", id, membre.getMatricule());
        membreRepository.delete(membre);
    }


    // --- Méthodes utilisées par d'autres services

    /**
     * Valide qu'un membre peut créer une réservation :
     * - pas de pénalité active
     * - pas de solde impayé
     * - délai de réservation respecté selon son type
     * - peut réserver sur ce site
     */
    public void validerDroitReservation(Membre membre, Long siteId, LocalDate dateMatch) {
        LocalDate today = LocalDate.now();
        AppProperties.Reservation resa = appProperties.getReservation();

        if (membre.aPenaliteActive(today)) {
            throw new BusinessException(String.format(
                    "Votre droit de réservation est suspendu jusqu'au  %s.",
                    membre.getPenaliteJusquA()
            ));
        }

        if (membre.aSoldeImpaye()) {
            throw new BusinessException(String.format(
                    "Vous avez un solde impayé de %s €. Veuillez régulariser votre situation pour pouvoir réserver.",
                    membre.getSoldeImpaye()
            ));
        }

        if (!membre.peutReserverSurSite(siteId)) {
            throw new BusinessException(
                    "Vous ne pouvez réserver que sur votre site d'appartenance."
            );
        }

        if (!membre.peutReserver(
                dateMatch,
                today,
                resa.getDelaiGlobalSemaines(),
                resa.getDelaiSiteSemaines(),
                resa.getDelaiLibreJours() )) {

            String delaiMsg = switch (membre.getTypeMembre()) {
                case GLOBAL -> resa.getDelaiGlobalSemaines() + " semaines";
                case SITE   -> resa.getDelaiSiteSemaines() + " semaines";
                case LIBRE  -> resa.getDelaiLibreJours() + " jours";
            };

            throw new BusinessException(String.format(
                    "Vous pouvez réserver au maximum %s à l'avance (type %s).",
                    delaiMsg, membre.getTypeMembre()
            ));
        }
    }

    public Membre getMembreOrThrow(Long id) {
        return membreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Membre introuvable : id=" + id));
    }

    public Membre getMembreByMatriculeOrThrow(String matricule) {
        return membreRepository.findByMatricule(matricule)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Membre introuvable : matricule=" + matricule));
    }

    // --- Helpers ---

    /**
     * Vérifie la cohérence entre le type de membre et son matricule.
     * - GLOBAL → commence par G
     * - SITE   → commence par S
     * - LIBRE  → commence par L
     */
    private void validerMaticuleCoherent(MembreRequest request) {
        char prefix = request.getMatricule().charAt(0);
        boolean coherent = switch (request.getTypeMembre()) {
            case GLOBAL -> prefix == 'G';
            case SITE   -> prefix == 'S';
            case LIBRE  -> prefix == 'L';
        };

        if (!coherent) {
            throw new BusinessException(String.format(
                    "Le matricule d'un membre %s doit commencer par '%s'.",
                    request.getTypeMembre(),
                    request.getTypeMembre().name().charAt(0)
            ));
        }

    }

    /**
     * Résout le site selon le type de membre :
     * - SITE  → site obligatoire
     * - GLOBAL / LIBRE → site null
     */
    private Site resoudreSite(MembreRequest request) {
        if (request.getTypeMembre() == TypeMembre.SITE) {
            if (request.getSiteId() == null) {
                throw new BusinessException(
                        "Un membre de type SITE doit être rattaché à un site."
                );
            }
            return siteService.getSiteOrThrow(request.getSiteId());
        }
        return null;
    }

    private void validerMatriculeUnique(String matricule, Long excludeId) {
        boolean doublon = (excludeId == null)
                ? membreRepository.existsByMatricule(matricule)
                : membreRepository.existsByMatriculeExcluding(matricule, excludeId);

        if (doublon) {
            throw new ConflictException("Ce matricule est déjà utilisé : " + matricule);
        }
    }

    private void validerEmailUnique(String email, Long excludeId) {
        boolean doublon = (excludeId == null)
                ? membreRepository.existsByEmail(email)
                : membreRepository.existsByEmailExcluding(email, excludeId);
        if (doublon) {
            throw new ConflictException("Cet email est déjà utilisé : " + email);
        }
    }




}
