package it.uniroma3.siwprogetto.service;

import it.uniroma3.siwprogetto.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service personalizzato per integrazione Spring Security con sistema utenti
 * 
 * Responsabilità:
 * - Implementazione UserDetailsService per Spring Security
 * - Mapping da User entity a UserDetails interface
 * - Gestione authorities/roles per autorizzazioni
 * - Supporto login con username OR email
 * - Configurazione bean UserDetailsService
 * 
 * Pattern Integration:
 * - Bridge tra domain model (User) e Spring Security (UserDetails)
 * - Strategy pattern per multiple authentication methods
 * - Separation of concerns: authentication vs authorization
 * 
 * Security Architecture:
 * - UserDetailsService = core interface Spring Security
 * - UserDetails = authenticated user representation
 * - GrantedAuthority = permissions/roles for authorization
 * - UsernameNotFoundException = authentication failure
 * 
 * NOTA Configurazione:
 * - @Configuration + @Service per dual purpose
 * - Bean definition per dependency injection
 * - Integration con SecurityConfig
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
@Configuration
@Service
public class CustomUserDetailsService implements UserDetailsService {

    /**
     * Service layer per business logic utenti
     * Delegazione per operazioni find user complesse
     */
    @Autowired
    private UserService userService;

    /**
     * Core method per Spring Security authentication
     * 
     * Processo authentication multi-step:
     * 1. Validazione input username/email
     * 2. Tentativo ricerca per username
     * 3. Fallback ricerca per email se username fail
     * 4. Mapping User -> UserDetails con authorities
     * 5. Return UserDetails per SecurityContext
     * 
     * @param username Username o email per login
     * @return UserDetails representation per Spring Security
     * @throws UsernameNotFoundException Se utente non trovato
     * 
     * Authentication Strategy:
     * - Prima tentativo: ricerca per username
     * - Secondo tentativo: ricerca per email
     * - Exception chain per diagnostics dettagliati
     * - Supporto login unificato (username OR email)
     * 
     * Role Mapping:
     * - rolesString formato CSV: "ADMIN,USER,DEALER"
     * - Conversione in GrantedAuthority con prefisso "ROLE_"
     * - Empty list se roles null/empty (default behavior)
     * - Stream processing per transformation
     * 
     * Security Considerations:
     * - Password hash gestita da User entity (mai plaintext)
     * - Authorities mapping per fine-grained permissions
     * - Exception handling per information disclosure prevention
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Validazione input parameter
        if (username == null || username.isEmpty()) {
            throw new UsernameNotFoundException("Username o email non forniti");
        }

        User user = null;

        // Tentativo 1: ricerca per username
        try {
            user = userService.findByUsername(username);
        } catch (RuntimeException e) {
            // Tentativo 2: fallback ricerca per email
            try {
                user = userService.findByEmail(username);
            } catch (RuntimeException ex) {
                // Both attempts failed
                throw new UsernameNotFoundException("Utente non trovato con email o username: " + username);
            }
        }

        // Double-check per null safety
        if (user == null) {
            throw new UsernameNotFoundException("Utente non trovato con email o username: " + username);
        }

        // Mapping roles string to GrantedAuthority list
        List<GrantedAuthority> authorities = user.getRolesString() != null && !user.getRolesString().isEmpty()
                ? Arrays.stream(user.getRolesString().split(","))
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.trim()))
                    .collect(Collectors.toList())
                : Collections.emptyList();

        // Return Spring Security UserDetails implementation
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(), // Password hash from database
                authorities
        );
    }

    /**
     * Bean definition per dependency injection
     * 
     * @return UserDetailsService instance per SecurityConfig
     * 
     * Spring Configuration:
     * - Method reference per performance
     * - Lambda alternative: username -> loadUserByUsername(username)
     * - Singleton scope by default
     * - Auto-wiring in SecurityConfig via @Autowired
     * 
     * Design Pattern:
     * - Factory method pattern per bean creation
     * - Self-reference per consistency
     * - Explicit bean definition per clarity
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return this::loadUserByUsername;
    }
}