package be.ephec.pdw.projetwebbackendpadel.entity;

import be.ephec.pdw.projetwebbackendpadel.enums.TypeMembre;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "membres")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Membre extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Pattern(
            regexp = "^[GSL]\\d{4,6}$",
            message = "Le matricule doit commencer par G, S ou L suivi de 4 à 6 chiffres"
    )
    @Column(nullable = false, unique = true, length = 20)
    private String matricule;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type_membre",  nullable = false, length = 10)
    private TypeMembre typeMembre;

    // Nullable : null pour GLOBAL et LIBRE, obligatoire pour SITE
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nom;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String prenom;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    @Column(name = "mot_de_passe_hash", nullable = false)
    private String motDePasseHash;

    @Builder.Default
    @DecimalMin("0.00")
    @Column(name = "solde_impaye", nullable = false, precision = 10, scale = 2)
    private BigDecimal soldeImpaye = BigDecimal.ZERO;

    // Null = aucune pénalité active
    @Column(name = "penalite_jusqua")
    private LocalDate penaliteJusquA;

     // --- Relations ---

    @Builder.Default
    @OneToMany(mappedBy = "organisateur", fetch = FetchType.LAZY)
    private List<Match> matchesOrganises = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "membre", fetch = FetchType.LAZY)
    private List<Participation> participations = new ArrayList<>();


    // --- Méthodes métier

    /**
     * Vérifie si le membre a une pénalité active à une date donnée.
     */
    public boolean aPenaliteActive(LocalDate date) {
        return penaliteJusquA != null && !penaliteJusquA.isBefore(date);
    }

    /**
     * Vérifie si le membre a un solde impayé bloquant.
     */
    public boolean aSoldeImpaye() {
        return soldeImpaye != null && soldeImpaye.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Vérifie si le membre peut réserver à une date cible,
     * selon son type (G/S/L) et la date du match.
     *
     * @param dateMatch     date du match souhaité
     * @param dateActuelle  date d'aujourd'hui
     * @param delaiGSemaines  délai membre GLOBAL (en semaines)
     * @param delaiSSemaines  délai membre SITE (en semaines)
     * @param delaiLJours     délai membre LIBRE (en jours)
     */
    public boolean peutReserver(
            LocalDate dateMatch,
            LocalDate dateActuelle,
            int delaiGSemaines,
            int delaiSSemaines,
            int delaiLJours) {

        LocalDate dateOuvertureReservation = switch (typeMembre) {
            case GLOBAL -> dateMatch.minusWeeks(delaiGSemaines);
            case SITE   -> dateMatch.minusWeeks(delaiSSemaines);
            case LIBRE  -> dateMatch.minusDays(delaiLJours);
        };

        return !dateActuelle.isBefore(dateOuvertureReservation);
    }

    /**
     * Vérifie que le membre peut réserver sur un site donné.
     * - GLOBAL et LIBRE : tous les sites
     * - SITE : uniquement son site rattaché
     */
    public boolean peutReserverSurSite(Long siteId) {
        return switch (typeMembre) {
            case GLOBAL, LIBRE -> true;
            case SITE -> site != null && site.getId().equals(siteId);
        };
    }

    /**
     * Applique une pénalité d'une semaine à partir d'aujourd'hui.
     */
    public void appliquerPenalite() {
        this.penaliteJusquA = LocalDate.now().plusWeeks(1);
    }

    /**
     * Ajoute un montant au solde impayé.
     */
    public void ajouterSoldeImpaye(BigDecimal montant) {
        this.soldeImpaye = this.soldeImpaye.add(montant);
    }

    /**
     * Réduit le solde impayé (lors d'un paiement).
     */
    public void reduireSoldeImpaye(BigDecimal montant) {
        this.soldeImpaye = this.soldeImpaye.subtract(montant)
                .max(BigDecimal.ZERO);
    }


}
