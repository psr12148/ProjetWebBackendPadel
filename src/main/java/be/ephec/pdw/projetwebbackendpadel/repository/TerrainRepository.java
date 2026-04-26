package be.ephec.pdw.projetwebbackendpadel.repository;

import be.ephec.pdw.projetwebbackendpadel.entity.Terrain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TerrainRepository extends JpaRepository<Terrain,Long> {

    /**
     * Tous les terrains d'un site, triés par numéro.
     */
    @Query("SELECT t FROM Terrain t WHERE t.site.id = :siteId ORDER BY t.numero")
    List<Terrain> findBySiteId(@Param("siteId") Long siteId);

    /**
     * Vérifie qu'un numéro de terrain n'est pas déjà pris sur ce site (création).
     */
    @Query("""
        SELECT COUNT(t) > 0 FROM Terrain t
        WHERE t.site.id = :siteId
          AND t.numero  = :numero
    """)
    boolean existsByNumeroOnSite(
            @Param("siteId") Long siteId,
            @Param("numero") int numero
    );

    /**
     * Vérifie l'unicité du numéro sur le site, en excluant le terrain en cours
     * de modification (mise à jour).
     */
    @Query("""
        SELECT COUNT(t) > 0 FROM Terrain t
        WHERE t.site.id = :siteId
          AND t.numero  = :numero
          AND t.id     <> :excludeId
    """)
    boolean existsByNumeroOnSiteExcluding(
            @Param("siteId")    Long siteId,
            @Param("numero")    int numero,
            @Param("excludeId") Long excludeId
    );

    /**
     * Terrains disponibles (sans match confirmé) à un créneau donné.
     * Utilisé lors de la création d'un match pour proposer les terrains libres.
     */
    @Query("""
        SELECT t FROM Terrain t
        WHERE t.site.id = :siteId
          AND NOT EXISTS (
              SELECT m FROM Match m
              WHERE m.terrain.id = t.id
                AND m.dateHeure  = :dateHeure
                AND m.statut    <> 'ANNULE'
          )
        ORDER BY t.numero
    """)
    List<Terrain> findDisponibles(
            @Param("siteId")    Long siteId,
            @Param("dateHeure") LocalDateTime dateHeure
    );

    /**
     * Compte le nombre de terrains sur un site.
     */
    @Query("SELECT COUNT(t) FROM Terrain t WHERE t.site.id = :siteId")
    long countBySiteId(@Param("siteId") Long siteId);
}
