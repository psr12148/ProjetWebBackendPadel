package be.ephec.pdw.projetwebbackendpadel.repository;

import be.ephec.pdw.projetwebbackendpadel.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DashboardRepository extends JpaRepository<Match, Long> {

    /** Nombre de matchs non annulés entre deux dates. */
    @Query("""
        SELECT COUNT(m) FROM Match m
        WHERE m.statut <> 'ANNULE'
          AND m.dateHeure >= :debut
          AND m.dateHeure <  :fin
    """)
    long countMatchsBetween(
            @Param("debut") LocalDateTime debut,
            @Param("fin")   LocalDateTime fin
    );

    /** Nombre de matchs par statut entre deux dates. */
    @Query("""
        SELECT COUNT(m) FROM Match m
        WHERE m.statut = :statut
          AND m.dateHeure >= :debut
          AND m.dateHeure <  :fin
    """)
    long countMatchsByStatutBetween(
            @Param("statut") String statut,
            @Param("debut")  LocalDateTime debut,
            @Param("fin")    LocalDateTime fin
    );

    /** Chiffre d'affaires (somme des participations confirmées) entre deux dates. */
    @Query("""
        SELECT COALESCE(SUM(p.montantPaye), 0) FROM Participation p
        WHERE p.statut    = 'CONFIRME'
          AND p.match.dateHeure >= :debut
          AND p.match.dateHeure <  :fin
    """)
    BigDecimal chiffreAffairesBetween(
            @Param("debut") LocalDateTime debut,
            @Param("fin")   LocalDateTime fin
    );

    /** Matchs publics avec au moins une place disponible. */
    @Query("""
        SELECT COUNT(m) FROM Match m
        WHERE m.typeMatch = 'PUBLIC'
          AND m.statut    = 'EN_ATTENTE'
          AND m.dateHeure > :now
          AND (
              SELECT COUNT(p) FROM Participation p
              WHERE p.match.id = m.id
                AND p.statut  <> 'LIBERE'
          ) < 4
    """)
    long countMatchsPublicsDisponibles(@Param("now") LocalDateTime now);


    /** Matchs privés EN_ATTENTE avec moins de 4 joueurs actifs — risque de bascule. */
    @Query("""
        SELECT COUNT(m) FROM Match m
        WHERE m.typeMatch = 'PRIVE'
          AND m.statut    = 'EN_ATTENTE'
          AND m.dateHeure > :now
          AND (
              SELECT COUNT(p) FROM Participation p
              WHERE p.match.id = m.id
                AND p.statut  <> 'LIBERE'
          ) < 4
    """)
    long countMatchsPrivesIncomplets(@Param("now") LocalDateTime now);


    /** Alertes J-1 : matchs privés incomplets dont la date est demain. */
    @Query("""
        SELECT COUNT(m) FROM Match m
        WHERE m.typeMatch = 'PRIVE'
          AND m.statut    = 'EN_ATTENTE'
          AND CAST(m.dateHeure AS date) = :demain
          AND (
              SELECT COUNT(p) FROM Participation p
              WHERE p.match.id = m.id
                AND p.statut  <> 'LIBERE'
          ) < 4
    """)
    long countAlertesMatchsPrivesDemain(@Param("demain") LocalDate demain);


    /** Alertes J-1 : participations non payées pour les matchs de demain. */
    @Query("""
        SELECT COUNT(p) FROM Participation p
        WHERE p.statut    = 'EN_ATTENTE'
          AND p.montantPaye < p.montantDu
          AND CAST(p.match.dateHeure AS date) = :demain
    """)
    long countAlertesPlacesNonPayeesDemain(@Param("demain") LocalDate demain);


    /** Nombre de créneaux occupés par site sur une période. */
    @Query("""
        SELECT m.terrain.site.id, COUNT(m) FROM Match m
        WHERE m.statut <> 'ANNULE'
          AND m.dateHeure >= :debut
          AND m.dateHeure <  :fin
        GROUP BY m.terrain.site.id
    """)
    List<Object[]> countCreneauxOccupesParSite(
            @Param("debut") LocalDateTime debut,
            @Param("fin")   LocalDateTime fin
    );


    /** Nombre total de membres avec solde impayé > 0. */
    @Query("SELECT COUNT(m) FROM Membre m WHERE m.soldeImpaye > 0")
    long countMembresAvecSoldeImpaye();


    /** Somme totale des soldes impayés. */
    @Query("SELECT COALESCE(SUM(m.soldeImpaye), 0) FROM Membre m WHERE m.soldeImpaye > 0")
    BigDecimal totalSoldesImpayes();


    /** Nombre de membres avec pénalité active. */
    @Query("""
        SELECT COUNT(m) FROM Membre m
        WHERE m.penaliteJusquA IS NOT NULL
          AND m.penaliteJusquA >= :today
    """)
    long countMembresAvecPenalite(@Param("today") LocalDate today);

}
