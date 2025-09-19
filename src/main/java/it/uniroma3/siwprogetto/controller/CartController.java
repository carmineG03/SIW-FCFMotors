package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.CartItem;
import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.service.CartService;
import it.uniroma3.siwprogetto.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller per la gestione del carrello della spesa.
 * Gestisce visualizzazione, aggiunta, rimozione e checkout di prodotti e abbonamenti.
 * Supporta sia utenti autenticati che anonimi (carrello vuoto).
 */
@Controller
public class CartController {

    /** Logger per tracciare le operazioni del carrello */
    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    // === DIPENDENZE INIETTATE ===

    /** Servizio per la gestione del carrello */
    private final CartService cartService;

    /** Servizio per la gestione degli utenti */
    private final UserService userService;

    /**
     * Costruttore con dependency injection.
     */
    @Autowired
    public CartController(CartService cartService, UserService userService) {
        this.cartService = cartService;
        this.userService = userService;
    }

    // === VISUALIZZAZIONE CARRELLO ===

    /**
     * Mostra la pagina del carrello con tutti gli articoli e i totali calcolati.
     * Supporta sia utenti autenticati che anonimi (carrello vuoto).
     * Calcola automaticamente sconti attivi e prezzi finali.
     * 
     * @param model Modello per la vista
     * @param principal Utente autenticato (opzionale)
     * @return Nome della vista cart
     */
    @GetMapping("/cart")
    public String showCartPage(Model model, Principal principal) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && 
                                 auth.isAuthenticated() && 
                                 !"anonymousUser".equals(auth.getPrincipal());

        logger.info("Accesso al carrello - Utente autenticato: {}, Username: {}", 
                   isAuthenticated, principal != null ? principal.getName() : "anonimo");

        model.addAttribute("isAuthenticated", isAuthenticated);

        // Gestione carrello per utenti non autenticati (vuoto)
        if (!isAuthenticated || principal == null) {
            logger.debug("Mostrando carrello vuoto per utente non autenticato");
            populateEmptyCartModel(model);
            return "cart";
        }

        try {
            // Carica utente autenticato
            User user = userService.findByUsername(principal.getName());
            if (user == null) {
                logger.error("Utente non trovato nel database: {}", principal.getName());
                populateEmptyCartModel(model);
                return "cart";
            }

            // Carica articoli del carrello
            List<CartItem> cartItems = cartService.getCartItems(user);
            logger.info("Carrello caricato per utente: {} - Articoli: {}", 
                       user.getUsername(), cartItems.size());

            // Calcola totali e prezzi scontati
            CartTotals totals = calculateCartTotals(cartItems);

            // Popola il modello per la vista
            model.addAttribute("cartItems", cartItems);
            model.addAttribute("cartCount", cartItems.size());
            model.addAttribute("originalTotal", totals.originalTotal);
            model.addAttribute("discountedTotal", totals.discountedTotal);
            model.addAttribute("discountedPrices", totals.discountedPrices);

            logger.debug("Totali carrello - Originale: €{}, Scontato: €{}, Articoli con sconto: {}", 
                        totals.originalTotal, totals.discountedTotal, totals.discountedPrices.size());

            return "cart";

        } catch (Exception e) {
            logger.error("Errore durante il caricamento del carrello per utente: {}", 
                        principal.getName(), e);
            populateEmptyCartModel(model);
            model.addAttribute("error", "Errore durante il caricamento del carrello");
            return "cart";
        }
    }

    // === GESTIONE ARTICOLI CARRELLO ===

    /**
     * Aggiunge un abbonamento al carrello dell'utente autenticato.
     * Verifica che l'utente sia autenticato e che l'abbonamento esista.
     * 
     * @param subscriptionId ID dell'abbonamento da aggiungere
     * @param principal Utente autenticato
     * @return ResponseEntity con risultato dell'operazione JSON
     */
    @PostMapping("/cart/add-subscription")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addSubscriptionToCart(@RequestParam Long subscriptionId, 
                                                                    Principal principal) {
        logger.info("Richiesta aggiunta abbonamento al carrello: subscriptionId={}, utente={}", 
                   subscriptionId, principal != null ? principal.getName() : "non autenticato");

        Map<String, Object> response = new HashMap<>();

        try {
            // Verifica autenticazione
            if (principal == null) {
                throw new IllegalArgumentException("Utente non autenticato");
            }

            // Carica e verifica esistenza utente
            User user = userService.findByUsername(principal.getName());
            if (user == null) {
                throw new IllegalArgumentException("Utente non trovato nel sistema");
            }

            // Verifica validità dell'ID abbonamento
            if (subscriptionId == null || subscriptionId <= 0) {
                throw new IllegalArgumentException("ID abbonamento non valido");
            }

            // Aggiunge abbonamento al carrello tramite il servizio
            cartService.addSubscriptionToCart(subscriptionId, user);

            // Risposta di successo
            response.put("success", true);
            response.put("message", "Abbonamento aggiunto al carrello con successo");

            logger.info("Abbonamento aggiunto al carrello con successo: subscriptionId={}, utente={}", 
                       subscriptionId, user.getUsername());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Errore di validazione nell'aggiunta abbonamento: subscriptionId={}, errore={}", 
                       subscriptionId, e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            logger.error("Errore interno durante aggiunta abbonamento: subscriptionId={}, utente={}", 
                        subscriptionId, principal != null ? principal.getName() : "null", e);
            response.put("success", false);
            response.put("error", "Errore interno durante l'aggiunta dell'abbonamento al carrello");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Aggiorna la quantità di un articolo specifico nel carrello.
     * Supporta sia aumenti che diminuzioni di quantità.
     * 
     * @param itemId ID dell'articolo nel carrello
     * @param body Corpo della richiesta JSON con la nuova quantità
     * @param principal Utente autenticato
     * @return ResponseEntity con risultato dell'operazione JSON
     */
    @PostMapping("/cart/update/{itemId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateQuantity(@PathVariable Long itemId, 
                                                             @RequestBody Map<String, Integer> body, 
                                                             Principal principal) {
        logger.info("Richiesta aggiornamento quantità carrello: itemId={}, utente={}", 
                   itemId, principal != null ? principal.getName() : "non autenticato");

        Map<String, Object> response = new HashMap<>();

        try {
            // Verifica autenticazione
            if (principal == null) {
                throw new IllegalArgumentException("Utente non autenticato");
            }

            // Carica e verifica esistenza utente
            User user = userService.findByUsername(principal.getName());
            if (user == null) {
                throw new IllegalArgumentException("Utente non trovato nel sistema");
            }

            // Verifica validità dell'ID articolo
            if (itemId == null || itemId <= 0) {
                throw new IllegalArgumentException("ID articolo non valido");
            }

            // Estrae e valida la quantità dal corpo della richiesta
            Integer quantity = body.get("quantity");
            if (quantity == null) {
                throw new IllegalArgumentException("Quantità non specificata");
            }
            if (quantity < 0) {
                throw new IllegalArgumentException("La quantità non può essere negativa");
            }
            if (quantity > 999) {
                throw new IllegalArgumentException("Quantità massima consentita: 999");
            }

            // Aggiorna la quantità tramite il servizio
            cartService.updateQuantity(itemId, quantity, user);

            // Risposta di successo
            response.put("success", true);
            response.put("message", "Quantità aggiornata con successo");
            response.put("newQuantity", quantity);

            logger.info("Quantità aggiornata con successo: itemId={}, nuovaQuantità={}, utente={}", 
                       itemId, quantity, user.getUsername());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Errore di validazione nell'aggiornamento quantità: itemId={}, errore={}", 
                       itemId, e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            logger.error("Errore interno durante aggiornamento quantità: itemId={}, utente={}", 
                        itemId, principal != null ? principal.getName() : "null", e);
            response.put("success", false);
            response.put("error", "Errore interno durante l'aggiornamento della quantità");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Rimuove completamente un articolo dal carrello.
     * L'articolo viene eliminato indipendentemente dalla quantità.
     * 
     * @param itemId ID dell'articolo da rimuovere
     * @param principal Utente autenticato
     * @return ResponseEntity con risultato dell'operazione JSON
     */
    @PostMapping("/cart/remove/{itemId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeFromCart(@PathVariable Long itemId, 
                                                             Principal principal) {
        logger.info("Richiesta rimozione articolo dal carrello: itemId={}, utente={}", 
                   itemId, principal != null ? principal.getName() : "non autenticato");

        Map<String, Object> response = new HashMap<>();

        try {
            // Verifica autenticazione
            if (principal == null) {
                throw new IllegalArgumentException("Utente non autenticato");
            }

            // Carica e verifica esistenza utente
            User user = userService.findByUsername(principal.getName());
            if (user == null) {
                throw new IllegalArgumentException("Utente non trovato nel sistema");
            }

            // Verifica validità dell'ID articolo
            if (itemId == null || itemId <= 0) {
                throw new IllegalArgumentException("ID articolo non valido");
            }

            // Rimuove l'articolo dal carrello tramite il servizio
            cartService.removeFromCart(itemId, user);

            // Risposta di successo
            response.put("success", true);
            response.put("message", "Articolo rimosso dal carrello con successo");

            logger.info("Articolo rimosso dal carrello con successo: itemId={}, utente={}", 
                       itemId, user.getUsername());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Errore di validazione nella rimozione articolo: itemId={}, errore={}", 
                       itemId, e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            logger.error("Errore interno durante rimozione articolo: itemId={}, utente={}", 
                        itemId, principal != null ? principal.getName() : "null", e);
            response.put("success", false);
            response.put("error", "Errore interno durante la rimozione dell'articolo");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // === GESTIONE CHECKOUT E PAGAMENTI ===

    /**
     * Inizia il processo di checkout per gli abbonamenti nel carrello.
     * Genera un ID transazione univoco e calcola il totale da pagare.
     * 
     * @param principal Utente autenticato
     * @param model Modello per la vista di pagamento
     * @return Nome della vista payment o error
     */
    @PostMapping("/cart/checkout-subscriptions")
    public String initiateCheckout(Principal principal, Model model) {
        logger.info("Avvio processo checkout per utente: {}", 
                   principal != null ? principal.getName() : "non autenticato");

        try {
            // Verifica autenticazione
            if (principal == null) {
                throw new IllegalArgumentException("Utente non autenticato per il checkout");
            }

            // Carica e verifica esistenza utente
            User user = userService.findByUsername(principal.getName());
            if (user == null) {
                throw new IllegalArgumentException("Utente non trovato nel sistema");
            }

            // Calcola il totale del carrello
            BigDecimal total = cartService.calculateSubtotal(user);
            if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Carrello vuoto o totale non valido");
            }

            // Genera ID transazione univoco per tracciare il pagamento
            String transactionId = "FCF_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

            // Popola il modello per la pagina di pagamento
            model.addAttribute("total", total);
            model.addAttribute("transactionId", transactionId);
            model.addAttribute("user", user);

            logger.info("Checkout iniziato con successo: utente={}, totale=€{}, transactionId={}", 
                       user.getUsername(), total, transactionId);

            return "payment";

        } catch (IllegalArgumentException e) {
            logger.warn("Errore di validazione durante checkout: utente={}, errore={}", 
                       principal != null ? principal.getName() : "null", e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "error";

        } catch (Exception e) {
            logger.error("Errore interno durante avvio checkout per utente: {}", 
                        principal != null ? principal.getName() : "null", e);
            model.addAttribute("error", "Errore interno durante l'avvio del processo di checkout");
            return "error";
        }
    }

    /**
     * Processa il pagamento e completa il checkout degli abbonamenti.
     * Finalizza la transazione e attiva gli abbonamenti acquistati.
     * 
     * @param transactionId ID univoco della transazione
     * @param principal Utente autenticato
     * @return ResponseEntity con risultato del pagamento JSON
     */
    @PostMapping("/cart/process-payment")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> processPayment(@RequestParam String transactionId, 
                                                             Principal principal) {
        logger.info("Elaborazione pagamento: transactionId={}, utente={}", 
                   transactionId, principal != null ? principal.getName() : "non autenticato");

        Map<String, Object> response = new HashMap<>();

        try {
            // Verifica autenticazione
            if (principal == null) {
                throw new IllegalArgumentException("Utente non autenticato per il pagamento");
            }

            // Carica e verifica esistenza utente
            User user = userService.findByUsername(principal.getName());
            if (user == null) {
                throw new IllegalArgumentException("Utente non trovato nel sistema");
            }

            // Valida transaction ID
            if (transactionId == null || transactionId.trim().isEmpty()) {
                throw new IllegalArgumentException("ID transazione non valido");
            }

            if (!transactionId.matches("^FCF_[A-Z0-9]{16}$")) {
                throw new IllegalArgumentException("Formato ID transazione non valido");
            }

            // Processa il checkout con l'ID transazione attraverso il servizio
            cartService.checkoutSubscriptions(user, transactionId);

            // Risposta di successo
            response.put("success", true);
            response.put("message", "Pagamento completato con successo! I tuoi abbonamenti sono stati attivati.");
            response.put("transactionId", transactionId);
            response.put("redirectUrl", "/account?payment=success");

            logger.info("Pagamento completato con successo: utente={}, transactionId={}", 
                       user.getUsername(), transactionId);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Errore di validazione durante pagamento: transactionId={}, errore={}", 
                       transactionId, e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (IllegalStateException e) {
            logger.error("Stato illegale durante pagamento: transactionId={}, errore={}", 
                        transactionId, e.getMessage());
            response.put("success", false);
            response.put("error", "Errore durante l'elaborazione: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (Exception e) {
            logger.error("Errore interno durante pagamento: transactionId={}, utente={}", 
                        transactionId, principal != null ? principal.getName() : "null", e);
            response.put("success", false);
            response.put("error", "Errore interno durante l'elaborazione del pagamento. Riprova più tardi.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // === METODI UTILITY PRIVATI ===

    /**
     * Popola il modello con un carrello vuoto per utenti non autenticati.
     * Utilizzato per mantenere coerenza nell'interfaccia utente.
     * 
     * @param model Modello da popolare con valori vuoti
     */
    private void populateEmptyCartModel(Model model) {
        model.addAttribute("cartItems", List.of());
        model.addAttribute("cartCount", 0);
        model.addAttribute("originalTotal", BigDecimal.ZERO);
        model.addAttribute("discountedTotal", BigDecimal.ZERO);
        model.addAttribute("discountedPrices", new HashMap<Long, BigDecimal>());
        
        logger.debug("Modello carrello vuoto popolato");
    }

    /**
     * Calcola i totali del carrello inclusi sconti attivi e prezzi individuali.
     * Gestisce sia abbonamenti che prodotti, applicando sconti validi.
     * 
     * @param cartItems Lista degli articoli nel carrello
     * @return Oggetto CartTotals con tutti i calcoli completi
     */
    private CartTotals calculateCartTotals(List<CartItem> cartItems) {
        CartTotals totals = new CartTotals();

        for (CartItem item : cartItems) {
            try {
                if (item.getSubscription() != null) {
                    // Calcoli per abbonamenti
                    BigDecimal price = BigDecimal.valueOf(item.getSubscription().getPrice());
                    totals.originalTotal = totals.originalTotal.add(price);

                    // Verifica e applica sconto se valido
                    if (isDiscountValid(item.getSubscription().getDiscount(), 
                                       item.getSubscription().getDiscountExpiry())) {
                        BigDecimal discountedPrice = calculateDiscountedPrice(price, 
                                                     item.getSubscription().getDiscount());
                        totals.discountedTotal = totals.discountedTotal.add(discountedPrice);
                        totals.discountedPrices.put(item.getId(), discountedPrice);
                        
                        logger.debug("Sconto applicato su abbonamento: itemId={}, prezzo originale=€{}, prezzo scontato=€{}", 
                                   item.getId(), price, discountedPrice);
                    } else {
                        totals.discountedTotal = totals.discountedTotal.add(price);
                        totals.discountedPrices.put(item.getId(), price);
                    }

                } else if (item.getProduct() != null) {
                    // Calcoli per prodotti (attualmente senza sconti)
                    BigDecimal price = item.getProduct().getPrice();
                    if (price != null) {
                        totals.originalTotal = totals.originalTotal.add(price);
                        totals.discountedTotal = totals.discountedTotal.add(price);
                        totals.discountedPrices.put(item.getId(), price);
                        
                        logger.debug("Prodotto aggiunto ai totali: itemId={}, prezzo=€{}", 
                                   item.getId(), price);
                    }
                } else {
                    logger.warn("Articolo carrello senza prodotto o abbonamento valido: itemId={}", 
                               item.getId());
                }

            } catch (Exception e) {
                logger.warn("Errore nel calcolo per articolo carrello ID {}: {}", 
                           item.getId(), e.getMessage());
                // Continua con gli altri articoli senza interrompere il calcolo
            }
        }

        logger.debug("Totali carrello calcolati - Originale: €{}, Scontato: €{}, Articoli: {}", 
                    totals.originalTotal, totals.discountedTotal, cartItems.size());

        return totals;
    }

    /**
     * Verifica se uno sconto è ancora valido e applicabile.
     * 
     * @param discount Percentuale di sconto (può essere null)
     * @param discountExpiry Data di scadenza dello sconto (può essere null)
     * @return true se lo sconto è valido e non scaduto
     */
    private boolean isDiscountValid(Double discount, LocalDate discountExpiry) {
        boolean isValid = discount != null && 
                         discount > 0 && 
                         discount <= 100 &&
                         discountExpiry != null && 
                         discountExpiry.isAfter(LocalDate.now());
        
        if (!isValid && discount != null) {
            logger.debug("Sconto non valido o scaduto: sconto={}%, scadenza={}", 
                        discount, discountExpiry);
        }
        
        return isValid;
    }

    /**
     * Calcola il prezzo scontato applicando la percentuale di sconto.
     * Utilizza precisione alta per evitare errori di arrotondamento.
     * 
     * @param originalPrice Prezzo originale dell'articolo
     * @param discountPercentage Percentuale di sconto da applicare (0-100)
     * @return Prezzo finale dopo l'applicazione dello sconto
     */
    private BigDecimal calculateDiscountedPrice(BigDecimal originalPrice, Double discountPercentage) {
        if (originalPrice == null || discountPercentage == null) {
            return originalPrice;
        }

        // Converte la percentuale in decimale (es: 20% -> 0.20)
        BigDecimal discountDecimal = new BigDecimal(discountPercentage.toString())
                .divide(new BigDecimal("100"), 10, BigDecimal.ROUND_HALF_UP);
        
        // Calcola il prezzo scontato: prezzo * (1 - sconto%)
        BigDecimal discountedPrice = originalPrice.multiply(BigDecimal.ONE.subtract(discountDecimal));
        
        // Arrotonda a 2 decimali per la visualizzazione
        return discountedPrice.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    // === CLASSE UTILITY PER I TOTALI ===

    /**
     * Classe di supporto per contenere tutti i calcoli e totali del carrello.
     * Evita la duplicazione di codice e facilita la manutenzione.
     */
    private static class CartTotals {
        /** Totale originale senza sconti */
        BigDecimal originalTotal = BigDecimal.ZERO;
        
        /** Totale finale con sconti applicati */
        BigDecimal discountedTotal = BigDecimal.ZERO;
        
        /** Mappa dei prezzi scontati per articolo (itemId -> prezzo finale) */
        Map<Long, BigDecimal> discountedPrices = new HashMap<>();
    }
}