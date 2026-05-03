package be.ephec.pdw.projetwebbackendpadel.security;

import be.ephec.pdw.projetwebbackendpadel.entity.Membre;
import be.ephec.pdw.projetwebbackendpadel.repository.MembreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MembreRepository membreRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Membre membre = membreRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Auncun membre avec l'email : " + email ));

        // Détection admin via matricule "ADMIN"
        // À remplacer par un champ role en base si besoin
        boolean isAdmin = "ADMIN".equalsIgnoreCase(membre.getMatricule());

        return new CustomUserDetails(membre, isAdmin);
    }

}
