package be.ephec.pdw.projetwebbackendpadel.entity;

import be.ephec.pdw.projetwebbackendpadel.enums.StatutMatch;
import be.ephec.pdw.projetwebbackendpadel.enums.StatutParticipation;
import be.ephec.pdw.projetwebbackendpadel.enums.TypeMatch;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "matchs",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_terrain_creneau",
                columnNames = { "terrain_id", "date_heure" }
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Match extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terrain_id", nullable = false)
    private Terrain terrain;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisateur_id", nullable = false)
    private Membre organisateur;

    @NotNull
    @Column(name = "date_heure", nullable = false)
    private LocalDateTime dateHeure;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type_match", nullable = false, length = 10)
    private TypeMatch typeMatch;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private StatutMatch statut = StatutMatch.EN_ATTENTE;

    @Builder.Default
    @Column(name = "montant_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal montantTotal = new BigDecimal("60.00");

    @Builder.Default
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Participation> participations = new ArrayList<>();


    // --- Méthodes métier ---

    public long nombreJoueursConfirmes() {
        return participations.stream()
                // On ne garde que les joueurs actifs (on exclut ceux qui ont annulé)
                .filter(p -> p.getStatut() != StatutParticipation.LIBERE)
                .count();
    }

    public long placesDisponibles() {
        return 4 - participations.stream()
                    // On ne garde que les joueurs actifs (on exclut ceux qui ont annulé)
                    .filter(p -> p.getStatut() != StatutParticipation.LIBERE)
                    .count();
    }

    public boolean estComplet() {
        return placesDisponibles() == 0;
    }

    public boolean membreParticipe(Long membreId) {
        return participations.stream()
                // On ne garde que les joueurs actifs (on exclut ceux qui ont annulé)
                .filter(p -> p.getStatut() != StatutParticipation.LIBERE)
                // On cherche si l'ID du MEMBRE correspond à l'ID recherché
                .anyMatch(p -> p.getMembre().getId().equals(membreId));
    }

    public BigDecimal soldeRestant() {
        BigDecimal paye = participations.stream()
                .map(Participation::getMontantPaye)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return montantTotal.subtract(paye).max(BigDecimal.ZERO);
    }
}
