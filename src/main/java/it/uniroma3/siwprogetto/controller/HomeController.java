package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.service.CartService;
import it.uniroma3.siwprogetto.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller per la gestione della home page e pagine di autenticazione.
 * Fornisce endpoints per login, registrazione e gestione stato utente.
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
@Controller
public class HomeController {

    /** Logger per tracciare l'accesso alle pagine */
    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    // === DIPENDENZE INIETTATE ===

    /** Servizio per la gestione del carrello */
    private final CartService cartService;

    /** Servizio per la gestione degli utenti */
    private final UserService userService;

    /**
     * Costruttore con dependency injection.
     * 
     * @param cartService Servizio per operazioni sul carrello
     * @param userService Servizio per operazioni utente
     */
    @Autowired
    public HomeController(CartService cartService, UserService userService) {
        this.cartService = cartService;
        this.userService = userService;
    }

    // === GESTIONE AUTENTICAZIONE ===

    /**
     * Endpoint alternativo per la pagina di login.
     * Fornisce la stessa funzionalità di /login con supporto per stato utente.
     * 
     * @param model Modello per la vista
     * @return Nome della vista login
     */
    @GetMapping("/user-login")
    public String showUserLoginPage(Model model) {
        logger.debug("Accesso alla pagina user-login");

        try {
            // Verifica stato di autenticazione corrente
            AuthenticationInfo authInfo = getCurrentAuthenticationInfo();
            
            // Popola il modello con le informazioni di autenticazione
            model.addAttribute("isAuthenticated", authInfo.isAuthenticated());
            model.addAttribute("pageTitle", "Accedi - FCF Motors");

            // Aggiunge conteggio carrello se utente autenticato
            if (authInfo.isAuthenticated() && authInfo.getUser() != null) {
                try {
                    int cartCount = cartService.getCartItems(authInfo.getUser()).size();
                    model.addAttribute("cartCount", cartCount);
                    
                    logger.debug("Utente autenticato su user-login: {} (carrello: {} articoli)", 
                               authInfo.getUser().getUsername(), cartCount);
                } catch (Exception e) {
                    logger.warn("Errore nel caricamento carrello per user-login: {}", e.getMessage());
                    model.addAttribute("cartCount", 0);
                }
            } else {
                model.addAttribute("cartCount", 0);
            }

            return "login";

        } catch (Exception e) {
            logger.error("Errore durante il caricamento della pagina user-login", e);
            model.addAttribute("isAuthenticated", false);
            model.addAttribute("cartCount", 0);
            model.addAttribute("error", "Errore durante il caricamento della pagina");
            return "login";
        }
    }

    /**
     * Mostra la pagina di registrazione utente con stato di autenticazione.
     * Fornisce form per la creazione di nuovi account.
     * 
     * @param model Modello per la vista
     * @return Nome della vista register
     */
    @GetMapping("/user-register")
    public String showUserRegisterPage(Model model) {
        logger.debug("Accesso alla pagina user-register");

        try {
            // Verifica stato di autenticazione corrente
            AuthenticationInfo authInfo = getCurrentAuthenticationInfo();
            
            // Se l'utente è già autenticato, potrebbe essere più appropriato reindirizzarlo
            if (authInfo.isAuthenticated()) {
                logger.info("Utente già autenticato tenta di accedere alla registrazione: {}", 
                           authInfo.getUser().getUsername());
                model.addAttribute("warning", "Sei già autenticato nel sistema.");
            }

            // Popola il modello con le informazioni di autenticazione
            model.addAttribute("isAuthenticated", authInfo.isAuthenticated());
            model.addAttribute("pageTitle", "Registrati - FCF Motors");

            // Aggiunge conteggio carrello se utente autenticato
            if (authInfo.isAuthenticated() && authInfo.getUser() != null) {
                try {
                    int cartCount = cartService.getCartItems(authInfo.getUser()).size();
                    model.addAttribute("cartCount", cartCount);
                    
                    logger.debug("Utente autenticato su user-register: {} (carrello: {} articoli)", 
                               authInfo.getUser().getUsername(), cartCount);
                } catch (Exception e) {
                    logger.warn("Errore nel caricamento carrello per user-register: {}", e.getMessage());
                    model.addAttribute("cartCount", 0);
                }
            } else {
                model.addAttribute("cartCount", 0);
            }

            // Aggiunge oggetto User vuoto per il form di registrazione
            model.addAttribute("user", new User());

            return "register";

        } catch (Exception e) {
            logger.error("Errore durante il caricamento della pagina user-register", e);
            model.addAttribute("isAuthenticated", false);
            model.addAttribute("cartCount", 0);
            model.addAttribute("user", new User());
            model.addAttribute("error", "Errore durante il caricamento della pagina");
            return "register";
        }
    }

    // === METODI UTILITY PRIVATI ===

    /**
     * Ottiene le informazioni di autenticazione correnti dell'utente.
     * Centralizza la logica di verifica autenticazione per evitare duplicazione.
     * 
     * @return Oggetto AuthenticationInfo con stato e dettagli utente
     */
    private AuthenticationInfo getCurrentAuthenticationInfo() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            
            // Verifica se l'autenticazione è valida e non anonima
            boolean isAuthenticated = auth != null && 
                                     auth.isAuthenticated() && 
                                     !"anonymousUser".equals(auth.getPrincipal());

            if (isAuthenticated) {
                // Carica i dettagli completi dell'utente dal database
                String username = auth.getName();
                User user = userService.findByUsername(username);
                
                if (user != null) {
                    logger.debug("Utente autenticato trovato: {} con ruoli: {}", 
                               username, user.getRolesString());
                    return new AuthenticationInfo(true, user);
                } else {
                    logger.warn("Utente autenticato non trovato nel database: {}", username);
                    // L'utente è autenticato ma non esiste più nel DB
                    return new AuthenticationInfo(false, null);
                }
            }

            return new AuthenticationInfo(false, null);

        } catch (Exception e) {
            logger.error("Errore durante il recupero delle informazioni di autenticazione", e);
            return new AuthenticationInfo(false, null);
        }
    }

    // === CLASSE UTILITY PER AUTENTICAZIONE ===

    /**
     * Classe di supporto per contenere le informazioni di autenticazione.
     * Evita la duplicazione di codice e facilita la manutenzione.
     */
    private static class AuthenticationInfo {
        private final boolean authenticated;
        private final User user;

        public AuthenticationInfo(boolean authenticated, User user) {
            this.authenticated = authenticated;
            this.user = user;
        }

        public boolean isAuthenticated() {
            return authenticated;
        }

        public User getUser() {
            return user;
        }
    }
}