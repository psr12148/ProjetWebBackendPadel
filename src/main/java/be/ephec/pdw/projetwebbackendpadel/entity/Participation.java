package be.ephec.pdw.projetwebbackendpadel.entity;

import be.ephec.pdw.projetwebbackendpadel.enums.StatutParticipation;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "participations",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_participation",
                // pour correspondre au changelog Liquibase
                columnNames = { "match_id", "membre_id" }
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Participation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private StatutParticipation statut = StatutParticipation.EN_ATTENTE;

    @Builder.Default
    @Column(name = "montant_du", nullable = false, precision = 10, scale = 2)
    private BigDecimal montantDu = new BigDecimal("15.00");

    @Builder.Default
    @Column(name = "montant_paye", nullable = false, precision = 10, scale = 2)
    private BigDecimal montantPaye = BigDecimal.ZERO;

    @Column(name = "date_paiement")
    private LocalDateTime datePaiement;


    // --- Méthodes métier ---

    public boolean estPayee() {
        return montantPaye.compareTo(montantDu) >= 0;
    }

    public void confirmer() {
        this.statut       = StatutParticipation.CONFIRME;
        this.datePaiement = LocalDateTime.now();  // ← cohérent avec LocalDateTime
    }

    public void liberer() {
        this.statut = StatutParticipation.LIBERE;
    }


}
