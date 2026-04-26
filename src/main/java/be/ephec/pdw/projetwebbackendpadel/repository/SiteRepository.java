package be.ephec.pdw.projetwebbackendpadel.repository;

import be.ephec.pdw.projetwebbackendpadel.entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {

    /**
     * Tous les sites d'une année civile donnée.
     */
    @Query("SELECT s FROM Site s WHERE s.anneeApplicable = :annee ORDER BY s.nom")
    List<Site> findByAnnee(@Param("annee") int annee);

    /**
     * Recherche par nom exact (insensible à la casse).
     */
    @Query("SELECT s FROM Site s WHERE LOWER(s.nom) = LOWER(:nom)")
    Optional<Site> findByNom(@Param("nom") String nom);

    /**
     * Vérifie l'unicité du nom pour la création (aucun site existant avec ce nom).
     */
    @Query("SELECT COUNT(s) > 0 FROM Site s WHERE LOWER(s.nom) = LOWER(:nom)")
    boolean existsByNom(@Param("nom") String nom);

    /**
     * Vérifie l'unicité du nom pour la mise à jour
     * (exclut le site en cours de modification).
     */
    @Query("""
        SELECT COUNT(s) > 0 FROM Site s
        WHERE LOWER(s.nom) = LOWER(:nom)
          AND s.id <> :excludeId
    """)
    boolean existsByNomAndIdNot(@Param("nom") String nom, @Param("excludeId") Long excludeId);

    /**
     * Sites ouverts à une date donnée :
     * exclut les sites ayant un jour de fermeture global (site IS NULL)
     * ou spécifique à ce site, à cette date.
     */
    @Query("""
        SELECT s FROM Site s
        WHERE s.anneeApplicable = :annee
          AND NOT EXISTS (
              SELECT j FROM JourFermeture j
              WHERE j.date = :date
                AND (j.site IS NULL OR j.site.id = s.id)
          )
        ORDER BY s.nom
    """)
    List<Site> findSitesOuverts(@Param("date") LocalDate date, @Param("annee") int annee);

    /**
     * Tous les sites avec leurs terrains chargés en une seule requête
     * (évite le N+1 sur Site → Terrain).
     */
    @Query("SELECT DISTINCT s FROM Site s LEFT JOIN FETCH s.terrains ORDER BY s.nom")
    List<Site> findAllWithTerrains();

    /**
     * Recherche textuelle sur le nom ou l'adresse (pour une future barre de recherche).
     */
    @Query("""
        SELECT s FROM Site s
        WHERE LOWER(s.nom)     LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(s.adresse) LIKE LOWER(CONCAT('%', :search, '%'))
        ORDER BY s.nom
    """)
    List<Site> search(@Param("search") String search);

    /**
     *
     * Récupère un site par son ID avec tous ses terrains et les matchs de ces terrains
     * chargés en une seule requête (évite le N+1 sur Site → Terrain → Match).
     */

    @Query("SELECT DISTINCT s FROM Site s " +
            "LEFT JOIN FETCH s.terrains t " +
            "LEFT JOIN FETCH t.matchs " +
            "WHERE s.id = :id")
    Optional<Site> findByIdWithTerrainsAndMatchs(@Param("id") Long id);
}
