package be.ephec.pdw.projetwebbackendpadel.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "terrains",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_terrain_site",
                columnNames = { "site_id", "numero" }
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Terrain extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id",  nullable = false)
    private Site site;

    @Min(value = 1, message = "Le numéro de terrain doit être >= 1")
    @Column(nullable = false)
    private int numero;

    @Size(max = 50)
    @Column(length = 50)
    private String nom;


    // --- Relation ---

    @Builder.Default
    @OneToMany(mappedBy = "terrain", fetch = FetchType.LAZY)
    private List<Match> matchs =  new ArrayList<>();

    /**
     * Retourne le nom d'affichage :
     * - le nom si renseigné (ex: "Terrain Central")
     * - sinon "Terrain N°X" (ex: "Terrain N°1")
     */
    public String getNomAffichage() {
        return (nom != null && !nom.isBlank())
                ? nom
                : "Terrain N°" + numero;
    }
}
