package it.uniroma3.siwprogetto.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handler personalizzato per gestire i fallimenti dell'autenticazione.
 * Reindirizza l'utente alla pagina di login con un messaggio di errore
 * e registra l'evento per motivi di sicurezza.
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    /** Logger per tracciare i tentativi di login falliti */
    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationFailureHandler.class);

    /**
     * Gestisce il fallimento dell'autenticazione.
     * Registra l'evento e reindirizza alla pagina di login con errore.
     * 
     * @param request La richiesta HTTP
     * @param response La risposta HTTP
     * @param exception L'eccezione che ha causato il fallimento
     * @throws IOException In caso di errori I/O
     * @throws ServletException In caso di errori servlet
     */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, 
                                       HttpServletResponse response,
                                       AuthenticationException exception) throws IOException, ServletException {
        
        // Estrae l'username tentato (se disponibile)
        String attemptedUsername = request.getParameter("username");
        
        // Log del tentativo di login fallito per motivi di sicurezza
        logger.warn("Tentativo di login fallito per utente: {} - Motivo: {}", 
                   attemptedUsername != null ? attemptedUsername : "sconosciuto", 
                   exception.getMessage());
        
        // Reindirizza alla pagina di login con parametro di errore
        // Il parametro ?error=true verrà utilizzato nel frontend per mostrare il messaggio
        response.sendRedirect("/login?error=true");
    }
}