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

@Configuration
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isEmpty()) {
            throw new UsernameNotFoundException("Username o email non forniti");
        }

        User user = null;

        // Cerca prima per username
        try {
            user = userService.findByUsername(username);
        } catch (RuntimeException e) {
            // Username non trovato, prova con email
            try {
                user = userService.findByEmail(username);
            } catch (RuntimeException ex) {
                throw new UsernameNotFoundException("Utente non trovato con email o username: " + username);
            }
        }

        if (user == null) {
            throw new UsernameNotFoundException("Utente non trovato con email o username: " + username);
        }

        List<GrantedAuthority> authorities = user.getRolesString() != null && !user.getRolesString().isEmpty()
                ? Arrays.stream(user.getRolesString().split(","))
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.trim()))
                .collect(Collectors.toList())
                : Collections.emptyList();

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return this::loadUserByUsername;
    }
}