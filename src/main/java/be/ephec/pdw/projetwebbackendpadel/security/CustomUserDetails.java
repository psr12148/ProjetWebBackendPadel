package be.ephec.pdw.projetwebbackendpadel.security;

import be.ephec.pdw.projetwebbackendpadel.entity.Membre;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long membreId;
    private final String matricule;
    private final String email;
    private final String motDePasseHash;
    private final boolean admin;

    public CustomUserDetails(Membre membre, boolean admin) {
        this.membreId       = membre.getId();
        this.matricule      = membre.getMatricule();
        this.email          = membre.getEmail();
        this.motDePasseHash = membre.getMotDePasseHash();
        this.admin          = admin;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (admin) {
            return List.of(
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_MEMBRE")
            );
        }
        return List.of(new SimpleGrantedAuthority("ROLE_MEMBRE"));
    }

    @Override public String getPassword()             { return motDePasseHash; }
    @Override public String getUsername()             { return email; }
    @Override public boolean isAccountNonExpired()    { return true; }
    @Override public boolean isAccountNonLocked()     { return true; }
    @Override public boolean isCredentialsNonExpired(){ return true; }
    @Override public boolean isEnabled()              { return true; }
}
