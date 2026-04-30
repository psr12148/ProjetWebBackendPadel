package be.ephec.pdw.projetwebbackendpadel.repository;

import be.ephec.pdw.projetwebbackendpadel.entity.Participation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipationRepository extends JpaRepository<Participation,Long> {

    /**
     * Participation d'un membre à un match spécifique.
     */
    @Query("""
        SELECT p FROM Participation p
        WHERE p.match.id  = :matchId
          AND p.membre.id = :membreId
    """)
    Optional<Participation> findByMatchAndMembre(
            @Param("matchId")  Long matchId,
            @Param("membreId") Long membreId
    );

    /**
     * Toutes les participations non libérées d'un match.
     */
    @Query("""
        SELECT p FROM Participation p
        WHERE p.match.id = :matchId
          AND p.statut  <> 'LIBERE'
    """)
    List<Participation> findActivesForMatch(@Param("matchId") Long matchId);

    /**
     * Participations non payées pour un match donné —
     * utilisé par le scheduler J-1.
     */
    @Query("""
        SELECT p FROM Participation p
        WHERE p.match.id = :matchId
          AND p.statut   = 'EN_ATTENTE'
          AND p.montantPaye < p.montantDu
    """)
    List<Participation> findNonPayeesForMatch(@Param("matchId") Long matchId);

    /**
     * Toutes les participations d'un membre.
     */
    @Query("""
        SELECT p FROM Participation p
        LEFT JOIN FETCH p.match m
        WHERE p.membre.id = :membreId
          AND p.statut   <> 'LIBERE'
        ORDER BY m.dateHeure DESC
    """)
    List<Participation> findByMembre(@Param("membreId") Long membreId);
}
