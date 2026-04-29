package be.ephec.pdw.projetwebbackendpadel.repository;

import be.ephec.pdw.projetwebbackendpadel.entity.Membre;
import be.ephec.pdw.projetwebbackendpadel.enums.TypeMembre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MembreRepository extends JpaRepository<Membre,Long> {

    /**
     * Recherche par matricule exact.
     */
    @Query("SELECT m FROM Membre m WHERE m.matricule = :matricule")
    Optional<Membre> findByMatricule(@Param("matricule") String matricule);

    /**
     * Recherche par email exact.
     */
    @Query("SELECT m FROM Membre m WHERE LOWER(m.email) = LOWER(:email)")
    Optional<Membre> findByEmail(@Param("email") String email);

    /**
     * Tous les membres d'un type donné.
     */
    @Query("SELECT m FROM Membre m WHERE m.typeMembre = :type ORDER BY m.nom, m.prenom")
    List<Membre> findByType(@Param("type") TypeMembre type);

    /**
     * Tous les membres rattachés à un site (type SITE uniquement).
     */
    @Query("""
        SELECT m FROM Membre m
        WHERE m.site.id   = :siteId
          AND m.typeMembre = 'SITE'
        ORDER BY m.nom, m.prenom
    """)
    List<Membre> findBySite(@Param("siteId") Long siteId);

    /**
     * Recherche textuelle sur nom, prénom ou matricule.
     */
    @Query("""
        SELECT m FROM Membre m
        WHERE LOWER(m.nom)       LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(m.prenom)    LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(m.matricule) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(m.email)     LIKE LOWER(CONCAT('%', :search, '%'))
        ORDER BY m.nom, m.prenom
    """)
    List<Membre> search(@Param("search") String search);

    /**
     * Unicité du matricule (création).
     */
    @Query("SELECT COUNT(m) > 0 FROM Membre m WHERE m.matricule = :matricule")
    boolean existsByMatricule(@Param("matricule") String matricule);

    /**
     * Unicité du matricule (mise à jour — exclut le membre en cours).
     */
    @Query("""
        SELECT COUNT(m) > 0 FROM Membre m
        WHERE m.matricule = :matricule AND m.id <> :excludeId
    """)
    boolean existsByMatriculeExcluding(
            @Param("matricule") String matricule,
            @Param("excludeId") Long excludeId
    );

    /**
     * Unicité de l'email (création).
     */
    @Query("SELECT COUNT(m) > 0 FROM Membre m WHERE LOWER(m.email) = LOWER(:email)")
    boolean existsByEmail(@Param("email") String email);

    /**
     * Unicité de l'email (mise à jour).
     */
    @Query("""
        SELECT COUNT(m) > 0 FROM Membre m
        WHERE LOWER(m.email) = LOWER(:email) AND m.id <> :excludeId
    """)
    boolean existsByEmailExcluding(
            @Param("email") String email,
            @Param("excludeId") Long excludeId
    );

    /**
     * Membres ayant une pénalité encore active à une date donnée.
     * Utilisé par le scheduler pour lever les pénalités expirées.
     */
    @Query("""
        SELECT m FROM Membre m
        WHERE m.penaliteJusquA IS NOT NULL
          AND m.penaliteJusquA >= :date
    """)
    List<Membre> findAvecPenaliteActive(@Param("date") LocalDate date);

    /**
     * Membres ayant un solde impayé > 0.
     */
    @Query("SELECT m FROM Membre m WHERE m.soldeImpaye > 0 ORDER BY m.soldeImpaye DESC")
    List<Membre> findAvecSoldeImpaye();

    /**
     * Mise à jour du solde impayé en masse (optimisation scheduler).
     */
    @Modifying
    @Query("""
        UPDATE Membre m
        SET m.soldeImpaye = m.soldeImpaye + :montant
        WHERE m.id = :membreId
    """)
    void addSoldeImpaye(
            @Param("membreId") Long membreId,
            @Param("montant") BigDecimal montant
    );
}
