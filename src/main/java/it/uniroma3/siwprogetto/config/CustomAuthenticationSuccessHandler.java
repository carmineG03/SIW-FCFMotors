package it.uniroma3.siwprogetto.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handler personalizzato per gestire il successo dell'autenticazione.
 * Esegue operazioni post-login come salvataggio di informazioni nella sessione
 * e redirect dell'utente alla pagina appropriata.
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    /** Logger per tracciare gli eventi di autenticazione */
    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationSuccessHandler.class);

    /**
     * Gestisce il successo dell'autenticazione.
     * Salva l'email dell'utente nella sessione e reindirizza alla pagina account.
     * 
     * @param request La richiesta HTTP
     * @param response La risposta HTTP
     * @param authentication Oggetto contenente i dettagli dell'autenticazione
     * @throws IOException In caso di errori I/O
     * @throws ServletException In caso di errori servlet
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                      HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
        
        // Ottiene la sessione HTTP corrente
        HttpSession session = request.getSession();
        
        // Estrae l'email dell'utente dall'oggetto Authentication
        String userEmail = authentication.getName();
        
        // Salva l'email nella sessione per uso futuro
        session.setAttribute("email", userEmail);
        
        // Log dell'evento di login riuscito
        logger.info("Login riuscito per utente: {}", userEmail);
        
        // Reindirizza l'utente alla pagina del suo account
        response.sendRedirect("/account");
    }
}