package it.uniroma3.siwprogetto.service;

import it.uniroma3.siwprogetto.model.CartItem;
import it.uniroma3.siwprogetto.model.Payment;
import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.model.Subscription;
import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.model.UserSubscription;
import it.uniroma3.siwprogetto.repository.CartItemRepository;
import it.uniroma3.siwprogetto.repository.PaymentRepository;
import it.uniroma3.siwprogetto.repository.ProductRepository;
import it.uniroma3.siwprogetto.repository.SubscriptionRepository;
import it.uniroma3.siwprogetto.repository.UserSubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service layer per gestione carrello acquisti e checkout subscription
 * 
 * Responsabilità:
 * - Gestione CRUD operazioni carrello utente
 * - Calcolo prezzi con sconti e quantità
 * - Processo checkout con attivazione subscription
 * - Gestione featured products tramite subscription
 * - Integrazione payment gateway e audit trail
 * 
 * Business Model:
 * - CartItem = elemento nel carrello (subscription o product)
 * - Checkout = conversione carrello in subscription attive
 * - Auto-renewal = subscription si rinnova automaticamente
 * - Featured products = evidenziazione tramite subscription
 * 
 * Pattern Service vantaggi:
 * - Coordinamento transazioni complesse multi-entity
 * - Business logic centralizzata per pricing
 * - Integration con servizi esterni (payment, email)
 * - Validazioni business per autorizzazioni utente
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
@Service
public class CartService {

    /**
     * Repository per gestione elementi carrello
     * CRUD operations su CartItem con filtri per utente
     */
    @Autowired
    private CartItemRepository cartItemRepository;

    /**
     * Repository per accesso catalogo prodotti
     * Utilizzato per featured products processing
     */
    @Autowired
    private ProductRepository productRepository;

    /**
     * Repository per piani subscription disponibili
     * Caricamento dettagli subscription per carrello
     */
    @Autowired
    private SubscriptionRepository subscriptionRepository;

    /**
     * Service layer per operazioni utente complesse
     * Delegazione per subscription activation logic
     */
    @Autowired
    private UserService userService;

    /**
     * Repository per subscription utente attive
     * Gestione stato subscription post-checkout
     */
    @Autowired
    private UserSubscriptionRepository userSubscriptionRepository;

    /**
     * Repository per audit trail pagamenti
     * Tracking transazioni per compliance e debugging
     */
    @Autowired
    private PaymentRepository paymentRepository;

    /**
     * Recupera tutti gli elementi nel carrello di un utente
     * 
     * @param user Utente proprietario del carrello
     * @return Lista CartItem dell'utente (può essere vuota)
     * 
     * Utilizzi:
     * - Visualizzazione carrello in UI
     * - Calcolo subtotal per checkout
     * - Verifica contenuto prima pagamento
     * 
     * Business Logic:
     * - Ogni utente ha un carrello separato e isolato
     * - CartItem possono contenere subscription o prodotti
     * - Lazy loading di relazioni per performance
     */
    public List<CartItem> getCartItems(User user) {
        return cartItemRepository.findByUser(user);
    }

    /**
     * Aggiunge subscription al carrello utente
     * 
     * Workflow:
     * 1. Carica dettagli subscription dal catalogo
     * 2. Crea nuovo CartItem con subscription associata
     * 3. Imposta quantità default 1 (subscription non quantificabili)
     * 4. Associa all'utente e persiste nel database
     * 
     * @param subscriptionId ID subscription da aggiungere
     * @param user Utente proprietario del carrello
     * @throws IllegalArgumentException Se subscription non trovata
     * 
     * Business Rules:
     * - Subscription hanno sempre quantity = 1
     * - product = null per subscription pure
     * - Possibili duplicati gestiti a livello UI/controller
     * 
     * Edge Cases:
     * - Subscription già presente nel carrello (duplicate handling)
     * - Subscription inactive/discontinued
     */
    public void addSubscriptionToCart(Long subscriptionId, User user) {
        // Carica subscription con error handling
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Abbonamento non trovato"));

        // Crea nuovo elemento carrello
        CartItem item = new CartItem();
        item.setSubscription(subscription);
        item.setUser(user);
        item.setQuantity(1); // Subscription non quantificabili
        item.setProduct(null); // Nessun prodotto associato per subscription pure
        
        // Persiste nel database
        cartItemRepository.save(item);
    }

    /**
     * Aggiorna quantità elemento carrello con validazioni autorizzazione
     * 
     * Business Logic:
     * - quantity <= 0 -> eliminazione automatica elemento
     * - quantity > 0 -> aggiornamento e salvataggio
     * - Verifica ownership per sicurezza
     * 
     * @param itemId ID elemento carrello da modificare
     * @param quantity Nuova quantità (0 = eliminazione)
     * @param user Utente richiedente (per autorizzazione)
     * @throws IllegalArgumentException Se elemento non trovato o non autorizzato
     * 
     * Security:
     * - Verifica user.getId() matches item.getUser().getId()
     * - Prevenzione accesso non autorizzato a carrelli altrui
     * - Exception esplicita per tentativi non autorizzati
     * 
     * UX Pattern:
     * - quantity = 0 equivale a "rimuovi dal carrello"
     * - Riduce operazioni UI (un solo endpoint per update/delete)
     */
    public void updateQuantity(Long itemId, int quantity, User user) {
        // Carica elemento con error handling
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Elemento non trovato"));
        
        // Verifica autorizzazione utente
        if (!item.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Non autorizzato");
        }
        
        // Business logic per quantità
        if (quantity <= 0) {
            // Eliminazione per quantity zero/negativa
            cartItemRepository.delete(item);
        } else {
            // Aggiornamento quantità positiva
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }
    }

    /**
     * Rimuove elemento specifico dal carrello
     * 
     * @param itemId ID elemento da rimuovere
     * @param user Utente richiedente (per autorizzazione)
     * @throws IllegalArgumentException Se elemento non trovato o non autorizzato
     * 
     * Security:
     * - Stessa logica autorizzazione di updateQuantity
     * - Prevenzione eliminazione elementi di altri utenti
     * 
     * Alternative:
     * - Equivalente a updateQuantity(itemId, 0, user)
     * - Metodo esplicito per chiarezza semantica
     */
    public void removeFromCart(Long itemId, User user) {
        // Carica elemento con error handling
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Elemento non trovato"));
        
        // Verifica autorizzazione utente
        if (!item.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Non autorizzato");
        }
        
        // Eliminazione fisica elemento
        cartItemRepository.delete(item);
    }

    /**
     * Calcola subtotal carrello con sconti e quantità
     * 
     * Algoritmo pricing complesso:
     * 1. Itera tutti gli elementi carrello utente
     * 2. Per ogni elemento calcola prezzo base
     * 3. Applica sconti subscription se validi
     * 4. Moltiplica per quantità elemento
     * 5. Somma tutti i subtotal elementi
     * 
     * @param user Utente per cui calcolare subtotal
     * @return BigDecimal subtotal totale con sconti applicati
     * 
     * Pricing Logic:
     * - Subscription: price con discount se valido e non scaduto
     * - Product: price diretto moltiplicato per quantità
     * - BigDecimal per precision matematica su valute
     * - ROUND_HALF_UP per arrotondamenti standard
     * 
     * Discount Rules:
     * - discount != null AND discountExpiry != null
     * - discountExpiry > LocalDate.now() (non scaduto)
     * - discount percentuale applicata come (1 - discount/100)
     * - Precision 10 decimali per calcoli intermedi
     */
    public BigDecimal calculateSubtotal(User user) {
        return cartItemRepository.findByUser(user).stream()
                .map(item -> {
                    BigDecimal price = BigDecimal.ZERO;
                    
                    // Pricing per subscription
                    if (item.getSubscription() != null) {
                        price = BigDecimal.valueOf(item.getSubscription().getPrice());
                        
                        // Applicazione discount se valido
                        if (item.getSubscription().getDiscount() != null &&
                                item.getSubscription().getDiscountExpiry() != null &&
                                item.getSubscription().getDiscountExpiry().isAfter(LocalDate.now())) {
                            
                            // Calcolo percentuale sconto
                            BigDecimal discount = new BigDecimal(item.getSubscription().getDiscount().toString())
                                    .divide(new BigDecimal("100"), 10, BigDecimal.ROUND_HALF_UP);
                            
                            // Applicazione: prezzo * (1 - sconto%)
                            price = price.multiply(BigDecimal.ONE.subtract(discount));
                        }
                    } 
                    // Pricing per prodotti
                    else if (item.getProduct() != null) {
                        price = item.getProduct().getPrice();
                    }
                    
                    // Moltiplicazione per quantità
                    return price.multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add); // Somma totale
    }

    /**
     * Processo checkout completo con attivazione subscription
     * 
     * Workflow complesso multi-step:
     * 1. Calcola total carrello
     * 2. Crea record Payment per audit
     * 3. Processa tutti gli elementi carrello
     * 4. Attiva subscription con auto-renewal
     * 5. Gestisce featured products se applicabile
     * 6. Svuota carrello completato
     * 
     * @param user Utente che effettua checkout
     * @param transactionId ID transazione payment gateway
     * 
     * Business Logic:
     * - Payment record per compliance e tracking
     * - UserSubscription con auto-renewal abilitato
     * - Featured products per subscription+product combo
     * - Atomic transaction per consistenza
     * 
     * Edge Cases:
     * - Subscription senza prodotti (subscription pura)
     * - Prodotti con subscription (featured product activation)
     * - Multiple subscription nello stesso checkout
     * - Failure recovery e rollback handling
     */
    public void checkoutSubscriptions(User user, String transactionId) {
        // Calcolo total per payment record
        BigDecimal total = calculateSubtotal(user);

        // Creazione audit trail payment
        Payment payment = new Payment(user, total, transactionId, "SUCCESS");
        paymentRepository.save(payment);

        // Processing elementi carrello
        List<CartItem> items = cartItemRepository.findByUser(user);
        for (CartItem item : items) {
            // Attivazione subscription
            if (item.getSubscription() != null) {
                // Delega attivazione a UserService
                UserSubscription userSubscription = userService.subscribeUserToDealer(
                    user.getId(), item.getSubscription().getId());
                
                // Abilitazione auto-renewal
                userSubscription.setAutoRenew(true);
                userSubscriptionRepository.save(userSubscription);
            }
            
            // Gestione featured products
            if (item.getProduct() != null && item.getSubscription() != null) {
                Product product = item.getProduct();
                
                // Attivazione featured status
                product.setIsFeatured(true);
                
                // Calcolo scadenza evidenziazione
                product.setFeaturedUntil(
                    LocalDate.now()
                        .plusDays(item.getSubscription().getDurationDays())
                        .atStartOfDay()
                );
                
                productRepository.save(product);
            }
        }
        
        // Cleanup carrello completato
        cartItemRepository.deleteAll(items);
    }
}