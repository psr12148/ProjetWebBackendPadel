package be.ephec.pdw.projetwebbackendpadel.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Site extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom du site est obligatoire")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nom;

    @NotBlank(message = "L'adresse est obligatoire")
    @Size(max = 255)
    @Column(nullable = false)
    private String adresse;

    @Min(value = 1, message = "Un site doit avoir au moins 1 terrain")
    @Max(50)
    @Column(name = "nb_terrains", nullable = false)
    private int nbTerrains;

    @NotNull(message = "L'heure d'ouverture est obligatoire")
    @Column(name = "heure_ouverture", nullable = false)
    private LocalTime heureOuverture;

    @NotNull(message = "L'heure de fermeture est obligatoire")
    @Column(name = "heure_fermeture", nullable = false)
    private LocalTime heureFermeture;

    @Min(2024)
    @Column(name = "annee_applicable", nullable = false)
    private int anneeApplicable;

    // --- Relations ---

    @Builder.Default
    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Terrain> terrains = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JourFermeture> joursFermeture = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "site")
    private List<Membre> membres = new ArrayList<>();

    // --- Méthodes métier ---
    public boolean estCreneauValide(LocalTime heureDebut, LocalTime heureFin) {
        return !heureDebut.isBefore(heureOuverture) && !heureFin.isAfter(heureFermeture);
    }

    public int nombreCreneauxParJour() {
        long minutes = java.time.Duration
                .between(heureOuverture, heureFermeture)
                .toMinutes();
        return (int) (minutes / 105);
    }


}
