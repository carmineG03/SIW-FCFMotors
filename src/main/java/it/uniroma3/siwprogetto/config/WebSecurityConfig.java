package it.uniroma3.siwprogetto.config;

import it.uniroma3.siwprogetto.service.CustomUserDetailsService;
import it.uniroma3.siwprogetto.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Configurazione della sicurezza dell'applicazione FCF Motors.
 * Gestisce autenticazione, autorizzazione e controllo degli accessi.
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class WebSecurityConfig {

    // === DIPENDENZE INIETTATE ===
    
    /** Servizio personalizzato per il caricamento dei dettagli utente */
    private final CustomUserDetailsService customUserDetailsService;
    
    /** Handler per gestire il successo dell'autenticazione */
    private final AuthenticationSuccessHandler customAuthenticationSuccessHandler;
    
    /** Handler per gestire i fallimenti dell'autenticazione */
    private final AuthenticationFailureHandler customAuthenticationFailureHandler;
    
    /** Encoder per la crittografia delle password */
    private final PasswordEncoder passwordEncoder;

    /**
     * Costruttore con dependency injection.
     * 
     * @param customUserDetailsService Servizio per i dettagli utente
     * @param customAuthenticationSuccessHandler Handler per successo login
     * @param customAuthenticationFailureHandler Handler per fallimento login
     * @param passwordEncoder Encoder per le password
     */
    public WebSecurityConfig(CustomUserDetailsService customUserDetailsService,
                             AuthenticationSuccessHandler customAuthenticationSuccessHandler,
                             AuthenticationFailureHandler customAuthenticationFailureHandler,
                             PasswordEncoder passwordEncoder) {
        this.customUserDetailsService = customUserDetailsService;
        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
        this.customAuthenticationFailureHandler = customAuthenticationFailureHandler;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Configura la catena di filtri di sicurezza.
     * Definisce le regole di accesso per URL, autenticazione e logout.
     * 
     * @param http Oggetto HttpSecurity per la configurazione
     * @return SecurityFilterChain configurata
     * @throws Exception In caso di errori di configurazione
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // === CONFIGURAZIONE AUTORIZZAZIONI ===
            .authorizeHttpRequests(auth -> auth
                // Risorse pubbliche - accessibili senza autenticazione
                .requestMatchers(
                    "/", "/index", "/login", "/register", 
                    "/forgot-password", "/reset-password",
                    "/products/**", "/dealers/**", 
                    "/css/**", "/js/**", "/image/**", "/favicon.ico"
                ).permitAll()
                
                // API REST pubbliche per dealer e immagini
                .requestMatchers("/rest/dealers/**", "/rest/api/dealers/**", "/rest/api/images/**").permitAll()
                
                // Operazioni POST pubbliche
                .requestMatchers(HttpMethod.POST, 
                    "/login", "/register", "/forgot-password", "/reset-password",
                    "/rest/dealers/**", "/rest/api/dealers/**", "/cart/**"
                ).permitAll()
                
                // Operazioni PUT pubbliche per API dealer
                .requestMatchers(HttpMethod.PUT, "/rest/api/dealers/**").permitAll()
                
                // Carrello - accessibile pubblicamente
                .requestMatchers("/cart/**").permitAll()
                
                // Account - richiede autenticazione
                .requestMatchers("/account").authenticated()
                
                // Messaggi privati - richiede autenticazione
                .requestMatchers("/private/messages/**").authenticated()
                
                // Area privata - solo utenti con ruolo PRIVATE
                .requestMatchers("/private/**").hasAnyAuthority(SecurityConstants.PRIVATE_ROLE)
                
                // Manutenzione privata - solo utenti PRIVATE
                .requestMatchers("/manutenzione/private").hasAnyAuthority(SecurityConstants.PRIVATE_ROLE)
                
                // Manutenzione dealer - solo dealer autorizzati
                .requestMatchers("/manutenzione/dealer").hasAuthority(SecurityConstants.DEALER_ROLE)
                
                // Area admin - solo amministratori
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // Tutto il resto richiede autenticazione
                .anyRequest().authenticated()
            )
            
            // === GESTIONE ECCEZIONI ===
            .exceptionHandling(exceptionHandling -> 
                exceptionHandling.accessDeniedHandler(
                    (request, response, accessDeniedException) -> 
                        response.sendRedirect("/index")
                )
            )
            
            // === CONFIGURAZIONE LOGIN ===
            .formLogin(formLogin -> formLogin
                .loginPage("/login")                           // Pagina di login personalizzata
                .loginProcessingUrl("/perform_login")          // URL per processare il login
                .usernameParameter("username")                 // Nome parametro username
                .passwordParameter("password")                 // Nome parametro password
                .successHandler(customAuthenticationSuccessHandler)  // Handler successo
                .failureHandler(customAuthenticationFailureHandler)  // Handler fallimento
                .failureUrl("/login?error=true")              // URL in caso di errore
                .defaultSuccessUrl("/account", true)          // Redirect dopo login
                .permitAll()                                  // Login accessibile a tutti
            )
            
            // === CONFIGURAZIONE REMEMBER ME ===
            .rememberMe(rememberMe -> rememberMe
                .key("FCF_MOTORS_REMEMBER_KEY")              // Chiave per remember me
                .tokenValiditySeconds(86400)                 // 24 ore di validità
                .rememberMeParameter("remember-me")          // Nome parametro checkbox
                .userDetailsService(customUserDetailsService) // Servizio per caricare utente
            )
            
            // === CONFIGURAZIONE LOGOUT ===
            .logout(logout -> logout
                .logoutUrl("/logout")                        // URL per logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessHandler((request, response, authentication) -> 
                    response.sendRedirect("/")               // Redirect dopo logout
                )
                .invalidateHttpSession(true)                 // Invalida sessione
                .deleteCookies("JSESSIONID")                // Elimina cookie sessione
                .clearAuthentication(true)                   // Pulisce autenticazione
                .permitAll()                                 // Logout accessibile a tutti
            );

        return http.build();
    }

    /**
     * Bean per le utilità di sicurezza.
     * 
     * @return Istanza di SecurityUtils
     */
    @Bean
    public SecurityUtils securityUtils() {
        return new SecurityUtils();
    }

    /**
     * Configura l'authentication manager con il servizio personalizzato
     * per i dettagli utente e l'encoder delle password.
     * 
     * @param auth Builder per la configurazione dell'autenticazione
     * @throws Exception In caso di errori di configurazione
     */
    @Autowired
    public void configureAuth(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(customUserDetailsService)
            .passwordEncoder(passwordEncoder);
    }
}