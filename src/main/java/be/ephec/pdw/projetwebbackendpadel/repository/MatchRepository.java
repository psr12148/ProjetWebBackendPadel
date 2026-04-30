package be.ephec.pdw.projetwebbackendpadel.repository;

import be.ephec.pdw.projetwebbackendpadel.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    /**
     * Matchs d'un site pour une date donnée, avec participations chargées.
     */
    @Query("""
        SELECT DISTINCT m FROM Match m
        LEFT JOIN FETCH m.participations p
        LEFT JOIN FETCH p.membre
        WHERE m.terrain.site.id = :siteId
          AND CAST(m.dateHeure AS date) = :date
          AND m.statut <> 'ANNULE'
        ORDER BY m.dateHeure
    """)
    List<Match> findBySiteAndDate(
            @Param("siteId") Long siteId,
            @Param("date") LocalDate date
    );

    /**
     * Matchs publics disponibles (places restantes) à partir d'aujourd'hui.
     */
    @Query("""
        SELECT m FROM Match m
        WHERE m.typeMatch = 'PUBLIC'
          AND m.statut    = 'EN_ATTENTE'
          AND m.dateHeure > :now
          AND (
              SELECT COUNT(p) FROM Participation p
              WHERE p.match.id = m.id
                AND p.statut  <> 'LIBERE'
          ) < 4
        ORDER BY m.dateHeure
    """)
    List<Match> findMatchsPublicsDisponibles(@Param("now") LocalDateTime now);

    /**
     * Matchs d'un membre (organisateur ou participant).
     */
    @Query("""
        SELECT DISTINCT m FROM Match m
        LEFT JOIN FETCH m.participations p
        WHERE m.organisateur.id = :membreId
           OR (p.membre.id = :membreId AND p.statut <> 'LIBERE')
        ORDER BY m.dateHeure DESC
    """)
    List<Match> findByMembre(@Param("membreId") Long membreId);

    /**
     * Vérifie qu'un terrain est libre à un créneau donné.
     */
    @Query("""
        SELECT COUNT(m) = 0 FROM Match m
        WHERE m.terrain.id = :terrainId
          AND m.dateHeure  = :dateHeure
          AND m.statut    <> 'ANNULE'
    """)
    boolean isTerrainDisponible(
            @Param("terrainId") Long terrainId,
            @Param("dateHeure") LocalDateTime dateHeure
    );

    /**
     * Matchs privés EN_ATTENTE dont la date est demain —
     * utilisé par le scheduler J-1.
     */
    @Query("""
        SELECT m FROM Match m
        LEFT JOIN FETCH m.participations p
        LEFT JOIN FETCH p.membre
        WHERE m.typeMatch = 'PRIVE'
          AND m.statut    = 'EN_ATTENTE'
          AND CAST(m.dateHeure AS date) = :demain
    """)
    List<Match> findPrivesEnAttenteForDemain(@Param("demain") LocalDate demain);

    /**
     * Matchs EN_ATTENTE dont la date est demain (tous types) —
     * pour vérifier les paiements J-1.
     */
    @Query("""
        SELECT m FROM Match m
        LEFT JOIN FETCH m.participations p
        LEFT JOIN FETCH p.membre
        WHERE m.statut    = 'EN_ATTENTE'
          AND CAST(m.dateHeure AS date) = :demain
    """)
    List<Match> findEnAttenteForDemain(@Param("demain") LocalDate demain);

    /**
     * Match avec toutes ses participations chargées (évite N+1).
     */
    @Query("""
        SELECT m FROM Match m
        LEFT JOIN FETCH m.participations p
        LEFT JOIN FETCH p.membre
        WHERE m.id = :id
    """)
    Optional<Match> findByIdWithParticipations(@Param("id") Long id);

}
