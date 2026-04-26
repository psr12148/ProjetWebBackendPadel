package be.ephec.pdw.projetwebbackendpadel.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "membre")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Membre extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "site_id")
    private Site site;
}
