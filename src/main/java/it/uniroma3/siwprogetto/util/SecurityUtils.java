package it.uniroma3.siwprogetto.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import java.util.Collection;

/**
 * Utility per la gestione della sicurezza e dei ruoli utente.
 * 
 * Questa classe fornisce metodi statici per verificare se l'utente autenticato
 * possiede un determinato ruolo. Utile nei controller, servizi o altre classi
 * dove è necessario effettuare controlli di autorizzazione custom.
 * 
 * Esempio d'uso:
 * if (SecurityUtils.hasRole("ROLE_ADMIN")) { ... }
 */
public class SecurityUtils {

    /**
     * Verifica se l'utente autenticato possiede il ruolo specificato.
     *
     * @param role il nome del ruolo da verificare (es. "ROLE_ADMIN")
     * @return true se l'utente ha il ruolo, false altrimenti
     */
    public static boolean hasRole(String role) {
        // Ottiene l'oggetto Authentication dal contesto di sicurezza corrente
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            // Nessun utente autenticato
            return false;
        }
        // Recupera la lista delle autorità (ruoli) dell'utente
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        // Controlla se tra le autorità è presente il ruolo richiesto
        for (GrantedAuthority authority : authorities) {
            if (authority.getAuthority().equals(role)) {
                return true;
            }
        }
        // Ruolo non trovato tra le autorità dell'utente
        return false;
    }
}