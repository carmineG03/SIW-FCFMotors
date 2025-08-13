package it.uniroma3.siwprogetto.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller globale per la gestione degli errori dell'applicazione.
 * Intercetta tutti gli errori HTTP e fornisce pagine di errore personalizzate
 * con messaggi informativi per l'utente.
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    /** Logger per tracciare gli errori dell'applicazione */
    private static final Logger logger = LoggerFactory.getLogger(ErrorController.class);

    /** Messaggio di errore generico per errori non specifici */
    private static final String GENERIC_ERROR_MESSAGE = "Si è verificato un errore imprevisto. Riprova più tardi.";

    /**
     * Gestisce tutti gli errori HTTP dell'applicazione.
     * Determina il tipo di errore e fornisce messaggi appropriati all'utente.
     * 
     * @param request Richiesta HTTP che ha generato l'errore
     * @param model Modello per la vista di errore
     * @return Nome della vista error
     */
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        // Estrae informazioni sull'errore dalla richiesta
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object errorMessage = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object requestUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        String userErrorMessage = GENERIC_ERROR_MESSAGE;
        String errorTitle = "Errore";
        int statusCode = 0;

        try {
            if (status != null) {
                statusCode = Integer.parseInt(status.toString());
                model.addAttribute("errorCode", statusCode);

                // Determina messaggio e titolo in base al codice di stato
                switch (statusCode) {
                    case 400:
                        errorTitle = "Richiesta Non Valida";
                        userErrorMessage = "La richiesta inviata non è valida. Verifica i dati inseriti.";
                        break;

                    case 401:
                        errorTitle = "Accesso Negato";
                        userErrorMessage = "È necessario effettuare il login per accedere a questa risorsa.";
                        break;

                    case 403:
                        errorTitle = "Accesso Vietato";
                        userErrorMessage = "Non hai i permessi necessari per accedere a questa pagina.";
                        break;

                    case 404:
                        errorTitle = "Pagina Non Trovata";
                        userErrorMessage = "La pagina richiesta non è stata trovata. Potrebbe essere stata spostata o eliminata.";
                        break;

                    case 405:
                        errorTitle = "Metodo Non Consentito";
                        userErrorMessage = "Il metodo utilizzato per questa richiesta non è consentito.";
                        break;

                    case 408:
                        errorTitle = "Timeout Richiesta";
                        userErrorMessage = "La richiesta ha impiegato troppo tempo. Riprova più tardi.";
                        break;

                    case 429:
                        errorTitle = "Troppe Richieste";
                        userErrorMessage = "Hai inviato troppe richieste in poco tempo. Attendi qualche minuto prima di riprovare.";
                        break;

                    case 500:
                        errorTitle = "Errore Interno del Server";
                        userErrorMessage = "Si è verificato un errore interno del server. Il nostro team è stato notificato.";
                        break;

                    case 502:
                        errorTitle = "Gateway Non Valido";
                        userErrorMessage = "Errore di comunicazione con il server. Riprova tra qualche minuto.";
                        break;

                    case 503:
                        errorTitle = "Servizio Non Disponibile";
                        userErrorMessage = "Il servizio è temporaneamente non disponibile per manutenzione. Riprova più tardi.";
                        break;

                    case 504:
                        errorTitle = "Timeout Gateway";
                        userErrorMessage = "Il server ha impiegato troppo tempo a rispondere. Riprova più tardi.";
                        break;

                    default:
                        errorTitle = "Errore " + statusCode;
                        userErrorMessage = "Si è verificato un errore imprevisto (codice: " + statusCode + "). Riprova più tardi.";
                        break;
                }

                // Log dell'errore per debugging (diverso livello in base alla gravità)
                if (statusCode >= 500) {
                    // Errori del server - livello ERROR
                    logger.error("Errore del server {} su URL: {} - Messaggio: {} - Eccezione: {}", 
                               statusCode, requestUri, errorMessage, 
                               exception != null ? exception.toString() : "Nessuna eccezione");
                } else if (statusCode >= 400) {
                    // Errori del client - livello WARN  
                    logger.warn("Errore del client {} su URL: {} - Messaggio: {}", 
                              statusCode, requestUri, errorMessage);
                } else {
                    // Altri errori - livello INFO
                    logger.info("Errore {} su URL: {} - Messaggio: {}", 
                              statusCode, requestUri, errorMessage);
                }

            } else {
                // Errore senza codice di stato specifico
                logger.error("Errore senza codice di stato su URL: {} - Messaggio: {} - Eccezione: {}", 
                           requestUri, errorMessage, 
                           exception != null ? exception.toString() : "Nessuna eccezione");
            }

        } catch (Exception e) {
            // Gestisce errori nella gestione degli errori (meta-errore)
            logger.error("Errore durante la gestione dell'errore principale", e);
            statusCode = 500;
            errorTitle = "Errore Interno";
            userErrorMessage = GENERIC_ERROR_MESSAGE;
        }

        // Popola il modello per la vista
        model.addAttribute("errorCode", statusCode);
        model.addAttribute("errorTitle", errorTitle);
        model.addAttribute("errorMessage", userErrorMessage);
        model.addAttribute("requestedUrl", requestUri);
        model.addAttribute("showDetails", shouldShowErrorDetails(statusCode));

        // Suggerimenti per l'utente in base al tipo di errore
        addErrorSuggestions(model, statusCode);

        logger.debug("Pagina di errore mostrata per codice: {} con titolo: {}", statusCode, errorTitle);

        return "error";
    }

    /**
     * Determina se mostrare i dettagli tecnici dell'errore all'utente.
     * I dettagli vengono mostrati solo per errori del client (4xx).
     * 
     * @param statusCode Codice di stato HTTP
     * @return true se mostrare i dettagli
     */
    private boolean shouldShowErrorDetails(int statusCode) {
        // Mostra dettagli solo per errori del client, non del server
        return statusCode >= 400 && statusCode < 500;
    }

    /**
     * Aggiunge suggerimenti contestuali per l'utente in base al tipo di errore.
     * Aiuta l'utente a capire cosa fare per risolvere il problema.
     * 
     * @param model Modello per la vista
     * @param statusCode Codice di stato HTTP
     */
    private void addErrorSuggestions(Model model, int statusCode) {
        String suggestion = "";
        String actionUrl = "";
        String actionText = "";

        switch (statusCode) {
            case 401:
                suggestion = "Effettua il login per accedere alla risorsa richiesta.";
                actionUrl = "/login";
                actionText = "Vai al Login";
                break;

            case 403:
                suggestion = "Contatta l'amministratore se ritieni di dover avere accesso a questa risorsa.";
                actionUrl = "/";
                actionText = "Torna alla Home";
                break;

            case 404:
                suggestion = "Verifica l'URL inserito o utilizza la navigazione del sito.";
                actionUrl = "/";
                actionText = "Torna alla Home";
                break;

            case 500:
                suggestion = "Il nostro team tecnico è stato notificato e risolverà il problema al più presto.";
                actionUrl = "/";
                actionText = "Torna alla Home";
                break;

            case 503:
                suggestion = "Il sito è in manutenzione. Riprova tra qualche minuto.";
                actionUrl = "/";
                actionText = "Riprova";
                break;

            default:
                suggestion = "Se il problema persiste, contatta il nostro supporto tecnico.";
                actionUrl = "/";
                actionText = "Torna alla Home";
                break;
        }

        model.addAttribute("suggestion", suggestion);
        model.addAttribute("actionUrl", actionUrl);
        model.addAttribute("actionText", actionText);
    }
}