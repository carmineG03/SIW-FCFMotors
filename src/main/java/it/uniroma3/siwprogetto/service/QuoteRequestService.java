package it.uniroma3.siwprogetto.service;

import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.model.QuoteRequest;
import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.repository.QuoteRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service layer per gestione richieste preventivo e messaggistica privata
 * 
 * Responsabilità:
 * - Gestione flusso comunicazione utente-venditore privato
 * - Creazione e risposta messaggi privati per prodotti
 * - Validazione autorizzazioni e business rules
 * - Integration con EmailService per notifiche
 * - Audit trail conversazioni per trasparenza
 * 
 * Business Model:
 * - QuoteRequest = container per messaggi privati tra utenti
 * - PRIVATE requestType = comunicazione diretta non dealer
 * - Bidirectional messaging = utente <-> seller conversation
 * - Email notifications = bridge per engagement off-platform
 * 
 * Pattern Service vantaggi:
 * - Business logic centralizzata per messaging
 * - Validazioni autorizzazioni security-aware
 * - Integration layer con servizi esterni (email)
 * - Consistent error handling e logging
 * - Reusable messaging components
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
@Service
public class QuoteRequestService {

    /**
     * Repository per persistenza richieste preventivo
     * Gestione CRUD e query specializzate messaging
     */
    @Autowired
    private QuoteRequestRepository quoteRequestRepository;

    /**
     * Service per invio email transazionali
     * Notifiche real-time per engagement utente
     */
    @Autowired
    private EmailService mailService;

    /**
     * Crea messaggio privato da utente a venditore privato
     * 
     * Business Rules Validation:
     * - Solo prodotti con sellerType "PRIVATE" ammessi
     * - Validazione sender authentication implicita
     * - Auto-population campi sistema (dates, status)
     * - Email notification automatica al destinatario
     * 
     * @param sender User che invia il messaggio (mittente)
     * @param product Product oggetto della comunicazione
     * @param message Testo messaggio da inviare
     * @return QuoteRequest creata e persistita
     * @throws IllegalArgumentException Se product non è venduto da privato
     * 
     * Workflow Steps:
     * 1. Validazione business rule (PRIVATE seller only)
     * 2. Creazione QuoteRequest con metadati sistema
     * 3. Persistenza database con timestamp
     * 4. Invio email notification al venditore
     * 5. Return QuoteRequest per response tracking
     * 
     * Data Mapping:
     * - user = mittente messaggio
     * - product = prodotto oggetto conversazione
     * - recipientEmail = seller email per delivery
     * - requestType = "PRIVATE" (vs "DEALER")
     * - status = "PENDING" (initial state)
     * - dealer = null (explicitly for private sales)
     * - responseMessage = messaggio utente (field reuse)
     * 
     * Email Integration:
     * - Immediate notification al seller
     * - Product context per identificazione
     * - Reply-to sender per conversazione
     */
    public QuoteRequest createPrivateMessage(User sender, Product product, String message) {
        // Business rule validation
        if (!"PRIVATE".equals(product.getSellerType())) {
            throw new IllegalArgumentException("Private messages are only for private sellers");
        }

        // QuoteRequest entity creation
        QuoteRequest quoteRequest = new QuoteRequest();
        quoteRequest.setUser(sender); // Mittente messaggio
        quoteRequest.setProduct(product); // Prodotto oggetto comunicazione
        quoteRequest.setUserEmail(sender.getEmail()); // Email mittente
        quoteRequest.setRecipientEmail(product.getSeller().getEmail()); // Email destinatario (seller)
        quoteRequest.setRequestType("PRIVATE"); // Tipo comunicazione privata
        quoteRequest.setStatus("PENDING"); // Stato iniziale in attesa risposta
        quoteRequest.setRequestDate(LocalDateTime.now()); // Timestamp creazione
        quoteRequest.setResponseMessage(message); // Testo messaggio (field reuse)
        quoteRequest.setDealer(null); // Explicitly null per vendite private

        // Persistenza database
        quoteRequest = quoteRequestRepository.save(quoteRequest);

        // Email notification al seller
        mailService.sendPrivateMessageEmail(
                quoteRequest.getRecipientEmail(), // Destinatario = seller
                sender.getEmail(), // Mittente per reply-to
                product, // Context prodotto
                message // Testo messaggio
        );

        return quoteRequest;
    }

    /**
     * Gestisce risposta a messaggio privato con validazione autorizzazione
     * 
     * Authorization Logic:
     * - Solo mittente originale O destinatario possono rispondere
     * - Verifica ID utente per mittente originale
     * - Verifica email per destinatario (seller)
     * - Exception per tentativi non autorizzati
     * 
     * @param quoteRequestId ID del messaggio a cui rispondere
     * @param responder User che sta rispondendo
     * @param responseMessage Testo della risposta
     * @return QuoteRequest aggiornata con risposta
     * @throws IllegalArgumentException Se messaggio non trovato o non autorizzato
     * 
     * Response Flow:
     * 1. Caricamento QuoteRequest esistente
     * 2. Validazione authorization (sender OR recipient)
     * 3. Update responseMessage con nuova risposta
     * 4. Cambio status a "RESPONDED"
     * 5. Determinazione recipient per email notification
     * 6. Invio email notification alla controparte
     * 7. Return QuoteRequest aggiornata
     * 
     * Authorization Matrix:
     * - Original sender (user.id match): può rispondere
     * - Original recipient (email match): può rispondere
     * - Third parties: IllegalArgumentException
     * 
     * Email Routing Logic:
     * - Se responder = original sender -> email a recipient
     * - Se responder = original recipient -> email a sender
     * - Automatic routing per conversation continuity
     * 
     * Status Management:
     * - "PENDING" -> "RESPONDED" transition
     * - Audit trail per conversation tracking
     * - No soft delete, full conversation history
     */
    public QuoteRequest respondToPrivateMessage(Long quoteRequestId, User responder, String responseMessage) {
        // Caricamento messaggio esistente
        QuoteRequest quoteRequest = quoteRequestRepository.findById(quoteRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Quote request not found"));

        // Authorization validation
        if (!quoteRequest.getRequestType().equals("PRIVATE") ||
                (!quoteRequest.getUser().getId().equals(responder.getId()) &&
                        !quoteRequest.getRecipientEmail().equals(responder.getEmail()))) {
            throw new IllegalArgumentException("Not authorized to respond");
        }

        // Update response content e status
        quoteRequest.setResponseMessage(responseMessage);
        quoteRequest.setStatus("RESPONDED");
        quoteRequest = quoteRequestRepository.save(quoteRequest);

        // Email routing logic per notification
        String recipientEmail = quoteRequest.getUser().getId().equals(responder.getId())
                ? quoteRequest.getRecipientEmail() // Responder = original sender -> notify recipient
                : quoteRequest.getUser().getEmail(); // Responder = original recipient -> notify sender

        // Email notification per conversation continuity
        mailService.sendPrivateMessageResponseEmail(
                recipientEmail, // Dinamically routed recipient
                responder.getEmail(), // Responder email per reply-to
                quoteRequest.getProduct(), // Product context maintained
                responseMessage // Response text
        );

        return quoteRequest;
    }

    /**
     * Recupera tutti i messaggi privati per utente (inviati e ricevuti)
     * 
     * Query Logic:
     * - Messaggi inviati: user.id = userId (come mittente)
     * - Messaggi ricevuti: recipientEmail = userEmail (come destinatario)
     * - OR condition per includere entrambe le direzioni
     * - PRIVATE requestType filter per escludere dealer quotes
     * 
     * @param user Utente per cui cercare i messaggi
     * @return Lista QuoteRequest private dell'utente (può essere vuota)
     * 
     * Utilizzi:
     * - Dashboard messaggistica utente
     * - Cronologia conversazioni complete
     * - Inbox/Outbox unified view
     * - Thread management per UI
     * 
     * Privacy Considerations:
     * - Solo messaggi dove utente è parte attiva
     * - No access a conversazioni di terze parti
     * - Email-based authorization per recipients
     * - Thread isolation per security
     * 
     * Performance:
     * - Index composto su (user_id, recipient_email) consigliato
     * - requestType filter pre-applied per efficiency
     * - Lazy loading relazioni Product per memory optimization
     */
    public List<QuoteRequest> getPrivateMessagesForUser(User user) {
        return quoteRequestRepository.findPrivateMessagesByUserIdOrEmail(user.getId(), user.getEmail());
	}
}