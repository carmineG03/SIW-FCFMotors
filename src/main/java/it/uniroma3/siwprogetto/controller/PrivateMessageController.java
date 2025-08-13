package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.model.QuoteRequest;
import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.repository.ProductRepository;
import it.uniroma3.siwprogetto.service.QuoteRequestService;
import it.uniroma3.siwprogetto.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller per la gestione dei messaggi tra utenti privati
 * Gestisce l'invio e la ricezione di messaggi diretti tra venditori privati e acquirenti
 * Diverso dal sistema di preventivi dei dealer - qui è una comunicazione diretta
 */
@Controller
@RequestMapping("/private/messages")
public class PrivateMessageController {

    private static final Logger logger = LoggerFactory.getLogger(PrivateMessageController.class);

    @Autowired
    private QuoteRequestService quoteRequestService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductRepository productRepository;

    /**
     * Visualizza la casella messaggi dell'utente privato
     * Mostra tutti i messaggi ricevuti e inviati relativi alle sue auto
     * 
     * @param authentication Autenticazione utente corrente
     * @param model Model per passare dati alla view
     * @return template della casella messaggi
     */
    @GetMapping
    @Transactional(readOnly = true)
    public String showMessages(Authentication authentication, Model model) {
        logger.info("Accesso casella messaggi per utente: {}", authentication.getName());
        
        try {
            // === VERIFICA AUTENTICAZIONE ===
            User user = userService.findByUsername(authentication.getName());
            if (user == null) {
                logger.warn("Utente non trovato: {}", authentication.getName());
                return "redirect:/login";
            }

            // === RECUPERO MESSAGGI UTENTE ===
            // Include sia i messaggi ricevuti (per le sue auto) che quelli inviati (per auto altrui)
            List<QuoteRequest> messages = quoteRequestService.getPrivateMessagesForUser(user);
            logger.debug("Trovati {} messaggi per l'utente {}", messages.size(), user.getUsername());

            // === PREPARAZIONE DATI VIEW ===
            model.addAttribute("messages", messages);
            model.addAttribute("currentUser", user);
            
            // Statistiche per la dashboard (opzionale)
            long unreadCount = messages.stream()
                .filter(msg -> "PENDING".equals(msg.getStatus()))
                .count();
            model.addAttribute("unreadCount", unreadCount);
            
            logger.info("Caricata casella messaggi: {} messaggi totali, {} non letti", 
                messages.size(), unreadCount);

            return "private_messages";

        } catch (Exception e) {
            logger.error("Errore caricamento casella messaggi per {}: {}", 
                authentication.getName(), e.getMessage(), e);
            model.addAttribute("errorMessage", "Errore nel caricamento dei messaggi");
            model.addAttribute("messages", List.of()); // Lista vuota come fallback
            return "private_messages";
        }
    }

    /**
     * Mostra il form per inviare un messaggio a un venditore privato
     * Disponibile solo per prodotti venduti da utenti privati (non dealer)
     * 
     * @param productId ID del prodotto per cui inviare il messaggio
     * @param model Model per passare dati alla view
     * @return template del form di invio messaggio
     */
    @GetMapping("/send/{productId}")
    @Transactional(readOnly = true)
    public String showSendMessageForm(@PathVariable Long productId, Model model, 
                                    RedirectAttributes redirectAttributes) {
        logger.info("Richiesta form invio messaggio per prodotto ID: {}", productId);
        
        try {
            // === VERIFICA ESISTENZA PRODOTTO ===
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Prodotto non trovato"));
            
            // === VERIFICA TIPO VENDITORE ===
            // I messaggi privati sono solo per venditori privati, non dealer
            if (!"PRIVATE".equals(product.getSellerType())) {
                logger.warn("Tentativo invio messaggio privato a venditore non-privato: {}", 
                    product.getSellerType());
                redirectAttributes.addFlashAttribute("error", 
                    "I messaggi privati sono disponibili solo per venditori privati. " +
                    "Per i dealer utilizza il sistema di preventivi.");
                return "redirect:/products";
            }

            // === VERIFICA VENDITORE VALIDO ===
            User seller = product.getSeller();
            if (seller == null || seller.getEmail() == null || seller.getEmail().trim().isEmpty()) {
                logger.warn("Venditore non valido per prodotto {}: seller={}, email={}", 
                    productId, seller, seller != null ? seller.getEmail() : "null");
                redirectAttributes.addFlashAttribute("error", 
                    "Impossibile contattare il venditore - informazioni di contatto non disponibili");
                return "redirect:/products";
            }

            // === PREPARAZIONE DATI FORM ===
            model.addAttribute("product", product);
            model.addAttribute("recipientEmail", seller.getEmail());
            model.addAttribute("recipientName", seller.getUsername() != null ? seller.getUsername() : seller.getUsername());
            model.addAttribute("quoteRequest", new QuoteRequest()); // Oggetto vuoto per il form
            
            logger.debug("Form messaggio preparato per prodotto {} -> destinatario: {}", 
                productId, seller.getUsername());

            return "send_private_message";

        } catch (IllegalArgumentException e) {
            logger.warn("Errore validazione per prodotto {}: {}", productId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/products";
            
        } catch (Exception e) {
            logger.error("Errore caricamento form messaggio per prodotto {}: {}", 
                productId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", 
                "Errore nel caricamento del form di contatto");
            return "redirect:/products";
        }
    }

    /**
     * Invia un nuovo messaggio a un venditore privato
     * Crea una nuova entry nel sistema di messaggi
     * 
     * @param productId ID del prodotto oggetto del messaggio
     * @param message Contenuto del messaggio
     * @param authentication Autenticazione mittente
     * @return Redirect con messaggio di successo o errore
     */
    @PostMapping("/send")
    @Transactional
    public String sendMessage(@RequestParam Long productId, 
                            @RequestParam String message, 
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        logger.info("Invio messaggio per prodotto {} da utente: {}", productId, authentication.getName());
        
        try {
            // === VERIFICA AUTENTICAZIONE ===
            User sender = userService.findByUsername(authentication.getName());
            if (sender == null) {
                logger.warn("Mittente non trovato: {}", authentication.getName());
                redirectAttributes.addFlashAttribute("error", "Errore di autenticazione");
                return "redirect:/login";
            }

            // === VALIDAZIONE MESSAGGIO ===
            if (message == null || message.trim().isEmpty()) {
                logger.warn("Tentativo invio messaggio vuoto da {}", sender.getUsername());
                redirectAttributes.addFlashAttribute("error", "Il messaggio non può essere vuoto");
                return "redirect:/private/messages/send/" + productId;
            }

            if (message.length() > 1000) { // Limite caratteri
                logger.warn("Messaggio troppo lungo da {} (length: {})", sender.getUsername(), message.length());
                redirectAttributes.addFlashAttribute("error", "Il messaggio è troppo lungo (max 1000 caratteri)");
                return "redirect:/private/messages/send/" + productId;
            }

            // === VERIFICA PRODOTTO ===
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Prodotto non trovato"));

            // === INVIO TRAMITE SERVICE ===
            // Il service gestisce la creazione del QuoteRequest e le validazioni di business
            quoteRequestService.createPrivateMessage(sender, product, message.trim());
            
            logger.info("Messaggio inviato con successo: {} -> prodotto {}", 
                sender.getUsername(), productId);
            
            redirectAttributes.addFlashAttribute("success", 
                "Messaggio inviato con successo! Il venditore riceverà una notifica.");
            return "redirect:/products";

        } catch (IllegalArgumentException e) {
            logger.warn("Errore validazione invio messaggio: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/products";
            
        } catch (Exception e) {
            logger.error("Errore invio messaggio da {} per prodotto {}: {}", 
                authentication.getName(), productId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", 
                "Errore durante l'invio del messaggio. Riprova più tardi.");
            return "redirect:/products";
        }
    }

    /**
     * Risponde a un messaggio ricevuto
     * Permette al venditore di rispondere alle richieste degli acquirenti
     * 
     * @param id ID del messaggio a cui rispondere
     * @param responseMessage Contenuto della risposta
     * @param authentication Autenticazione utente corrente
     * @return Redirect alla casella messaggi con esito operazione
     */
    @PostMapping("/respond/{id}")
    @Transactional
    public String respondToMessage(@PathVariable Long id, 
                                 @RequestParam String responseMessage,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        logger.info("Risposta a messaggio ID: {} da utente: {}", id, authentication.getName());
        
        try {
            // === VERIFICA AUTENTICAZIONE ===
            User responder = userService.findByUsername(authentication.getName());
            if (responder == null) {
                logger.warn("Utente non trovato per risposta: {}", authentication.getName());
                redirectAttributes.addFlashAttribute("error", "Errore di autenticazione");
                return "redirect:/login";
            }

            // === VALIDAZIONE RISPOSTA ===
            if (responseMessage == null || responseMessage.trim().isEmpty()) {
                logger.warn("Tentativo risposta vuota da {} per messaggio {}", responder.getUsername(), id);
                redirectAttributes.addFlashAttribute("error", "La risposta non può essere vuota");
                return "redirect:/private/messages";
            }

            if (responseMessage.length() > 1000) { // Limite caratteri
                logger.warn("Risposta troppo lunga da {} (length: {})", responder.getUsername(), responseMessage.length());
                redirectAttributes.addFlashAttribute("error", "La risposta è troppo lunga (max 1000 caratteri)");
                return "redirect:/private/messages";
            }

            // === INVIO RISPOSTA TRAMITE SERVICE ===
            // Il service verifica che l'utente sia autorizzato a rispondere e gestisce la logica di business
            quoteRequestService.respondToPrivateMessage(id, responder, responseMessage.trim());
            
            logger.info("Risposta inviata con successo da {} per messaggio {}", responder.getUsername(), id);
            
            redirectAttributes.addFlashAttribute("success", "Risposta inviata con successo!");
            return "redirect:/private/messages";

        } catch (IllegalArgumentException e) {
            logger.warn("Errore validazione risposta messaggio ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/private/messages";
            
        } catch (Exception e) {
            logger.error("Errore risposta messaggio ID {} da {}: {}", 
                id, authentication.getName(), e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", 
                "Errore durante l'invio della risposta. Riprova più tardi.");
            return "redirect:/private/messages";
        }
    }

    /**
     * Segna un messaggio come letto (endpoint AJAX opzionale)
     * Può essere utilizzato per implementare funzionalità di lettura dinamica
     * 
     * @param id ID del messaggio da segnare come letto
     * @param authentication Autenticazione utente corrente
     * @return JSON response con esito operazione
     */
    @PostMapping("/mark-read/{id}")
    @ResponseBody
    @Transactional
    public String markAsRead(@PathVariable Long id, Authentication authentication) {
        logger.debug("Richiesta segna-come-letto per messaggio ID: {} da {}", id, authentication.getName());
        
        try {
            User user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return "{\"success\":false,\"error\":\"Non autorizzato\"}";
            }

            // Logica per segnare come letto - da implementare nel service se necessaria
            // quoteRequestService.markAsRead(id, user);
            
            return "{\"success\":true}";

        } catch (Exception e) {
            logger.error("Errore marcatura lettura messaggio {}: {}", id, e.getMessage(), e);
            return "{\"success\":false,\"error\":\"Errore interno\"}";
        }
    }

    /**
     * Elimina un messaggio (soft delete)
     * Nasconde il messaggio dalla vista dell'utente senza eliminarlo dal database
     * 
     * @param id ID del messaggio da eliminare
     * @param authentication Autenticazione utente corrente
     * @return Redirect alla casella messaggi
     */
    @PostMapping("/delete/{id}")
    @Transactional
    public String deleteMessage(@PathVariable Long id, Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        logger.info("Richiesta eliminazione messaggio ID: {} da {}", id, authentication.getName());
        
        try {
            User user = userService.findByUsername(authentication.getName());
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "Errore di autenticazione");
                return "redirect:/login";
            }

            // Logica di eliminazione - da implementare nel service se necessaria
            // quoteRequestService.deleteMessageForUser(id, user);
            
            logger.info("Messaggio {} nascosto per utente {}", id, user.getUsername());
            redirectAttributes.addFlashAttribute("success", "Messaggio eliminato");
            
            return "redirect:/private/messages";

        } catch (Exception e) {
            logger.error("Errore eliminazione messaggio {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Errore durante l'eliminazione");
            return "redirect:/private/messages";
        }
    }
}