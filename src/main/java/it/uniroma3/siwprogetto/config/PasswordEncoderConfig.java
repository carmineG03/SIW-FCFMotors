package it.uniroma3.siwprogetto.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configurazione per l'encoder delle password.
 * Definisce il bean PasswordEncoder utilizzato per crittografare e verificare le password.
 * 
 * Utilizza BCrypt che è considerato uno degli algoritmi più sicuri per l'hashing
 * delle password, con salt automatico e resistente agli attacchi rainbow table.
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * Crea e configura il bean PasswordEncoder.
     * 
     * BCryptPasswordEncoder fornisce:
     * - Hash sicuro delle password
     * - Salt automatico per ogni password
     * - Protezione contro attacchi a forza bruta
     * - Compatibilità con Spring Security
     * 
     * @return Istanza configurata di BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}