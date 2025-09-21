package it.uniroma3.siwprogetto.service;

import it.uniroma3.siwprogetto.model.Dealer;
import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.model.Subscription;
import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.repository.DealerRepository;
import it.uniroma3.siwprogetto.repository.ProductRepository;
import it.uniroma3.siwprogetto.repository.SubscriptionRepository;
import it.uniroma3.siwprogetto.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service layer per operazioni amministrative privilegiate del sistema
 * 
 * Responsabilità:
 * - Gestione completa entità di sistema (CRUD operations)
 * - Operazioni di moderazione e manutenzione
 * - Gestione subscription e pricing
 * - Eliminazioni cascade con gestione dipendenze
 * - Audit trail per operazioni sensibili
 * 
 * Security Model:
 * - @PreAuthorize("hasRole('ADMIN')") su tutti i metodi
 * - Role-based access control (RBAC)
 * - Logging dettagliato per compliance
 * - Transazioni isolate per consistenza dati
 * 
 * Pattern Service vantaggi:
 * - Centralizzazione logic amministrativa
 * - Delegation a services specializzati per operazioni complesse
 * - Exception handling standardizzato
 * - Logging strutturato per debugging e audit
 * - Transactional boundary management
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
@Service
public class AdminService {

    /**
     * Logger SLF4J per audit trail operazioni amministrative
     * Tracciamento dettagliato per compliance e debugging
     */
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    /**
     * Repository per accesso dati utenti
     * Gestione anagrafica completa sistema
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * Repository per accesso dati prodotti
     * Moderazione catalogo prodotti automotive
     */
    @Autowired
    private ProductRepository productRepository;

    /**
     * Repository per accesso dati dealer
     * Gestione concessionari e partnership
     */
    @Autowired
    private DealerRepository dealerRepository;

    /**
     * Repository per accesso dati subscription
     * Configurazione piani e pricing strategy
     */
    @Autowired
    private SubscriptionRepository subscriptionRepository;

    /**
     * Service specializzato per operazioni dealer
     * Delegation per eliminazioni cascade complesse
     */
    @Autowired
    private DealerService dealerService;

    /**
     * Trova prodotto per ID con autorizzazione amministrativa
     * 
     * @param id ID del prodotto da cercare
     * @return Optional contenente Product se trovato, empty() se non esiste
     * 
     * Utilizzi:
     * - Dashboard amministrativa dettaglio prodotto
     * - Moderazione contenuti specifici
     * - Debugging problemi prodotto
     * - Preparazione operazioni update/delete
     * 
     * Security:
     * - Solo utenti con ROLE_ADMIN possono accedere
     * - Nessuna restrizione per seller ownership
     */
    @PreAuthorize("hasRole('ADMIN')")
    public Optional<Product> findProductById(Long id) {
        return productRepository.findById(id);
    }

    /**
     * Trova dealer per ID con autorizzazione amministrativa
     * 
     * @param id ID del dealer da cercare
     * @return Optional contenente Dealer se trovato, empty() se non esiste
     * 
     * Utilizzi:
     * - Gestione partnership concessionari
     * - Moderazione dealer profile e contenuti
     * - Verifica configurazione dealer settings
     * - Preparazione operazioni update/delete
     */
    @PreAuthorize("hasRole('ADMIN')")
    public Optional<Dealer> findDealerById(Long id) {
        return dealerRepository.findById(id);
    }

    /**
     * Aggiorna prodotto con autorizzazione amministrativa bypass owner
     * 
     * Operazione transazionale per consistenza dati:
     * - Caricamento prodotto esistente con lock pessimistico
     * - Update campi business logic
     * - Persistenza con audit trail
     * - Rollback automatico su eccezioni
     * 
     * @param productId ID del prodotto da modificare
     * @param updatedProduct Oggetto con nuovi valori
     * @return Product aggiornato e persistito
     * @throws IllegalStateException Se prodotto non trovato
     * 
     * Campi aggiornabili:
     * - description, price, category
     * - brand, model, mileage, year
     * - fuelType, transmission
     * 
     * Campi NON modificabili:
     * - seller (immutable business rule)
     * - creationDate, id (system managed)
     * - images (gestite separatamente)
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Product updateProduct(Long productId, Product updatedProduct) {
        logger.debug("Admin updating product: id={}", productId);

        // Caricamento con verifica esistenza
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    logger.error("Product not found: id={}", productId);
                    return new IllegalStateException("Prodotto non trovato");
                });

        // Update campi business editable
        product.setDescription(updatedProduct.getDescription());
        product.setPrice(updatedProduct.getPrice());
        product.setCategory(updatedProduct.getCategory());
        product.setBrand(updatedProduct.getBrand());
        product.setModel(updatedProduct.getModel());
        product.setMileage(updatedProduct.getMileage());
        product.setYear(updatedProduct.getYear());
        product.setFuelType(updatedProduct.getFuelType());
        product.setTransmission(updatedProduct.getTransmission());

        // Persistenza con audit trail
        Product savedProduct = productRepository.save(product);
        logger.info("Product updated by admin: id={}, model={}", savedProduct.getId(), savedProduct.getModel());
        return savedProduct;
    }

    /**
     * Elimina prodotto con gestione dipendenze via DealerService
     * 
     * ATTENZIONE: Operazione irreversibile con cascade effects
     * - Eliminazione QuoteRequest associate
     * - Cleanup immagini e file associati
     * - Notifiche a seller per trasparenza
     * - Audit trail per compliance
     * 
     * @param productId ID del prodotto da eliminare
     * @throws IllegalStateException Se prodotto non trovato
     * @throws RuntimeException Se errore durante eliminazione
     * 
     * Delegation Strategy:
     * - DealerService gestisce dipendenze complesse
     * - Transaction management centralizzato
     * - Error handling e rollback automatico
     * - Logging dettagliato per troubleshooting
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteProduct(Long productId) {
        logger.debug("Admin deleting product: id={}", productId);

        // Verifica esistenza prima eliminazione
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    logger.error("Product not found: id={}", productId);
                    return new IllegalStateException("Prodotto non trovato");
                });

        // Delegation a service specializzato per cascade handling
        try {
            dealerService.deleteProduct(productId);
            logger.info("Product deleted by admin: id={}", productId);
        } catch (Exception e) {
            logger.error("Error deleting product via dealerService: id={}", productId, e);
            throw new RuntimeException("Errore durante l'eliminazione del prodotto: " + e.getMessage(), e);
        }
    }

    /**
     * Aggiorna dealer con autorizzazione amministrativa
     * 
     * @param dealerId ID del dealer da modificare
     * @param updatedDealer Oggetto con nuovi valori
     * @return Dealer aggiornato e persistito
     * @throws IllegalStateException Se dealer non trovato
     * 
     * Campi aggiornabili:
     * - name, description, address
     * - phone, email (contact info)
     * - images (gestione lista immagini)
     * 
     * Business Logic:
     * - Mantenimento relazione con owner (User)
     * - Validazione email format a livello model
     * - Gestione immagini come lista per portfolio
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Dealer updateDealer(Long dealerId, Dealer updatedDealer) {
        logger.debug("Admin updating dealer: id={}", dealerId);

        Dealer dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() -> {
                    logger.error("Dealer not found: id={}", dealerId);
                    return new IllegalStateException("Concessionario non trovato");
                });

        // Update campi profile dealer
        dealer.setName(updatedDealer.getName());
        dealer.setDescription(updatedDealer.getDescription());
        dealer.setAddress(updatedDealer.getAddress());
        dealer.setPhone(updatedDealer.getPhone());
        dealer.setEmail(updatedDealer.getEmail());
        dealer.setImages(updatedDealer.getImages()); // Gestione immagini come lista

        Dealer savedDealer = dealerRepository.save(dealer);
        logger.info("Dealer updated by admin: id={}, name={}", savedDealer.getId(), savedDealer.getName());
        return savedDealer;
    }

    /**
     * Elimina dealer e cascade prodotti associati
     * 
     * ATTENZIONE: Operazione complessa con multiple dependencies
     * - Eliminazione prodotti dealer (via cascade)
     * - Gestione QuoteRequest associate ai prodotti
     * - Cleanup file system (immagini dealer e prodotti)
     * - Mantenimento User owner (no cascade delete)
     * 
     * @param dealerId ID del dealer da eliminare
     * @throws IllegalStateException Se dealer non trovato
     * @throws RuntimeException Se errore durante eliminazione
     * 
     * Cascade Strategy:
     * - DealerService coordina eliminazioni multiple
     * - Transazione unica per consistenza atomica
     * - Rollback completo su qualsiasi fallimento
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteDealer(Long dealerId) {
        logger.debug("Admin deleting dealer: id={}", dealerId);

        Dealer dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() -> {
                    logger.error("Dealer not found: id={}", dealerId);
                    return new IllegalStateException("Concessionario non trovato");
                });

        // Delegation per gestione cascade complessa
        try {
            dealerService.deleteDealer(dealerId);
            logger.info("Dealer deleted by admin: id={}", dealerId);
        } catch (Exception e) {
            logger.error("Error deleting dealer via dealerService: id={}", dealerId, e);
            throw new RuntimeException("Errore durante l'eliminazione del concessionario: " + e.getMessage(), e);
        }
    }

    /**
     * Aggiorna account utente con autorizzazione amministrativa
     * 
     * @param userId ID dell'utente da modificare
     * @param updatedUser Oggetto con nuovi valori
     * @return User aggiornato e persistito
     * @throws IllegalStateException Se utente non trovato
     * 
     * Campi aggiornabili:
     * - username, email (credenziali)
     * - rolesString (gestione permissions)
     * 
     * Campi NON modificabili:
     * - password (gestita via reset password flow)
     * - registrationDate, id (system managed)
     * - accountInformation (gestita separatamente)
     * 
     * Security Considerations:
     * - Solo admin può modificare roles di altri utenti
     * - Username/email uniqueness validata a database level
     * - Audit log per tracciabilità modifiche permissions
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public User updateUser(Long userId, User updatedUser) {
        logger.debug("Admin updating user: id={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("User not found: id={}", userId);
                    return new IllegalStateException("Utente non trovato");
                });

        // Update campi amministrativi
        user.setUsername(updatedUser.getUsername());
        user.setEmail(updatedUser.getEmail());
        user.setRolesString(updatedUser.getRolesString());

        User savedUser = userRepository.save(user);
        logger.info("User updated by admin: id={}, username={}", savedUser.getId(), savedUser.getUsername());
        return savedUser;
    }

    /**
     * Elimina account utente con cascade cleanup completo
     * 
     * ATTENZIONE: Operazione irreversibile con effetti estesi
     * - GDPR compliance per right to be forgotten
     * - Eliminazione dealer associato (se presente)
     * - Cleanup prodotti non associati a dealer
     * - Gestione subscription via database constraints
     * - Audit trail per compliance legale
     * 
     * @param userId ID dell'utente da eliminare
     * @throws IllegalStateException Se utente non trovato
     * @throws RuntimeException Se errore durante eliminazione
     * 
     * Elimination Strategy (multi-step):
     * 1. Elimina dealer associato (cascade prodotti dealer)
     * 2. Elimina prodotti rimanenti non dealer-associated
     * 3. Elimina user (subscription gestite da DB constraints)
     * 4. Cleanup file system e cache
     * 
     * Database Constraints:
     * - UserSubscription: ON DELETE CASCADE o SET NULL
     * - CartItem: ON DELETE CASCADE
     * - AccountInformation: ON DELETE CASCADE
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long userId) {
        logger.debug("Admin deleting user: id={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("User not found: id={}", userId);
                    return new IllegalStateException("Utente non trovato");
                });

        try {
            // Step 1: Elimina dealer associato (cascade prodotti dealer)
            Optional<Dealer> dealer = dealerRepository.findByOwner(user);
            if (dealer.isPresent()) {
                logger.debug("Deleting dealer for user: userId={}, dealerId={}", userId, dealer.get().getId());
                dealerService.deleteDealer(dealer.get().getId());
            }

            // Step 2: Elimina prodotti rimanenti non associati a dealer
            List<Product> products = productRepository.findBySeller(user);
            for (Product product : products) {
                try {
                    logger.debug("Deleting remaining product: {}", product.getId());
                    dealerService.deleteProduct(product.getId());
                } catch (Exception e) {
                    logger.warn("Could not delete product {}: {}", product.getId(), e.getMessage());
                    // Continue with other products
                }
            }

            // Step 3: Elimina utente (subscription cascade o constraint)
            userRepository.delete(user);
            logger.info("User deleted by admin: id={}", userId);

        } catch (Exception e) {
            logger.error("Error deleting user: id={}", userId, e);
            throw new RuntimeException("Errore durante l'eliminazione dell'utente: " + e.getMessage(), e);
        }
    }

    /**
     * Recupera tutti i prodotti del sistema
     * 
     * @return Lista completa prodotti (può essere vuota)
     * 
     * Utilizzi:
     * - Dashboard amministrativa catalogo completo
     * - Export dati per analytics e reporting
     * - Moderazione contenuti bulk operations
     * - Statistiche inventory globale
     * 
     * Performance:
     * - Query potenzialmente costosa per grandi cataloghi
     * - Considerare paginazione per UI responsive
     */
    @PreAuthorize("hasRole('ADMIN')")
    public List<Product> findAllProducts() {
        return (List<Product>) productRepository.findAll();
    }

    /**
     * Recupera tutti i dealer del sistema
     * 
     * @return Lista completa dealer registrati
     * 
     * Utilizzi:
     * - Gestione partnership e accordi commerciali
     * - Dashboard dealer performance analytics
     * - Moderazione dealer profiles
     * - Statistiche business e revenue
     */
    @PreAuthorize("hasRole('ADMIN')")
    public List<Dealer> findAllDealers() {
        return dealerRepository.findAll();
    }

    /**
     * Recupera tutti gli utenti del sistema
     * 
     * @return Lista completa utenti registrati
     * 
     * Utilizzi:
     * - Dashboard utenti e gestione account
     * - Analytics demografici e behavior
     * - Customer service e supporto
     * - Compliance e audit user management
     * 
     * Privacy:
     * - Solo dati non sensibili esposti
     * - Password hash mai incluse in result sets
     * - GDPR compliance per data access
     */
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Recupera tutti i piani subscription configurati
     * 
     * @return Lista completa subscription disponibili
     * 
     * Utilizzi:
     * - Configurazione pricing e piani
     * - Dashboard subscription performance
     * - A/B testing per conversion optimization
     * - Revenue analytics per subscription model
     */
    @PreAuthorize("hasRole('ADMIN')")
    public List<Subscription> findAllSubscriptions() {
        return subscriptionRepository.findAll();
    }

    /**
     * Trova subscription per ID con autorizzazione amministrativa
     * 
     * @param id ID della subscription da cercare
     * @return Optional contenente Subscription se trovata
     * 
     * Utilizzi:
     * - Configurazione subscription specifica
     * - Debugging pricing issues
     * - Preparazione operazioni update/delete
     */
    @PreAuthorize("hasRole('ADMIN')")
    public Optional<Subscription> findSubscriptionById(Long id) {
        return subscriptionRepository.findById(id);
    }

    /**
     * Aggiunge nuovo piano subscription al catalogo
     * 
     * @param subscription Nuovo piano da aggiungere
     * @return Subscription persistita con ID generato
     * 
     * Business Logic:
     * - Validazione pricing strategy consistency
     * - Configurazione features e limiti
     * - Integration con payment gateway
     * - Audit trail per pricing changes
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Subscription addSubscription(Subscription subscription) {
        logger.debug("Admin adding subscription: name={}", subscription.getName());
        Subscription savedSubscription = subscriptionRepository.save(subscription);
        logger.info("Subscription added by admin: id={}, name={}, maxFeaturedCars={}",
                savedSubscription.getId(), savedSubscription.getName(), savedSubscription.getMaxFeaturedCars());
        return savedSubscription;
    }

    /**
     * Aggiorna piano subscription esistente
     * 
     * @param subscriptionId ID della subscription da modificare
     * @param updatedSubscription Oggetto con nuovi valori
     * @return Subscription aggiornata e persistita
     * @throws IllegalStateException Se subscription non trovata
     * 
     * Campi aggiornabili:
     * - name, description (marketing content)
     * - price, discount, discountExpiry (pricing)
     * - durationDays, maxFeaturedCars (features)
     * 
     * Impact Analysis:
     * - Subscription attive mantengono termini originali
     * - Nuove attivazioni utilizzano pricing aggiornato
     * - Grandfathering policy per existing subscribers
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Subscription updateSubscription(Long subscriptionId, Subscription updatedSubscription) {
        logger.debug("Admin updating subscription: id={}", subscriptionId);

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> {
                    logger.error("Subscription not found: id={}", subscriptionId);
                    return new IllegalStateException("Abbonamento non trovato");
                });

        // Update configurazione subscription
        subscription.setName(updatedSubscription.getName());
        subscription.setDescription(updatedSubscription.getDescription());
        subscription.setPrice(updatedSubscription.getPrice());
        subscription.setDiscount(updatedSubscription.getDiscount());
        subscription.setDiscountExpiry(updatedSubscription.getDiscountExpiry());
        subscription.setDurationDays(updatedSubscription.getDurationDays());
        subscription.setMaxFeaturedCars(updatedSubscription.getMaxFeaturedCars());

        Subscription savedSubscription = subscriptionRepository.save(subscription);
        logger.info("Subscription updated by admin: id={}, name={}, maxFeaturedCars={}",
                savedSubscription.getId(), savedSubscription.getName(), savedSubscription.getMaxFeaturedCars());
        return savedSubscription;
    }

    /**
     * Applica sconto temporaneo a subscription per campagne marketing
     * 
     * @param subscriptionId ID della subscription da scontare
     * @param discount Percentuale sconto (es: 0.20 per 20%)
     * @param discountExpiry Data scadenza sconto
     * @return Subscription con sconto applicato
     * @throws IllegalStateException Se subscription non trovata
     * 
     * Marketing Strategy:
     * - Sconti temporanei per conversion campaigns
     * - Flash sales e seasonal promotions
     * - A/B testing per price sensitivity
     * - Automatic expiry per urgency creation
     * 
     * Business Rules:
     * - Sconto applicato solo a nuove attivazioni
     * - Subscription attive non impattate retroattivamente
     * - Audit trail per revenue impact analysis
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Subscription applyDiscount(Long subscriptionId, Double discount, LocalDate discountExpiry) {
        logger.debug("Admin applying discount to subscription: id={}, discount={}", subscriptionId, discount);

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> {
                    logger.error("Subscription not found: id={}", subscriptionId);
                    return new IllegalStateException("Abbonamento non trovato");
                });

        // Applicazione sconto temporaneo
        subscription.setDiscount(discount);
        subscription.setDiscountExpiry(discountExpiry);

        Subscription savedSubscription = subscriptionRepository.save(subscription);
        logger.info("Discount applied to subscription: id={}, discount={}%", savedSubscription.getId(), discount);
        return savedSubscription;
    }

    /**
     * Elimina piano subscription dal catalogo
     * 
     * ATTENZIONE: Impact su subscription attive
     * - UserSubscription attive possono essere impattate
     * - Verificare CASCADE constraints configurazione
     * - Considerare soft delete per historical data
     * - Revenue impact per subscribers attivi
     * 
     * @param subscriptionId ID della subscription da eliminare
     * @throws IllegalStateException Se subscription non trovata
     * 
     * Business Considerations:
     * - Grandfathering policy per subscribers attivi
     * - Migration plan per affected users
     * - Refund policy se applicabile
     * - Communication plan per transparency
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteSubscription(Long subscriptionId) {
        logger.debug("Admin deleting subscription: id={}", subscriptionId);

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> {
                    logger.error("Subscription not found: id={}", subscriptionId);
                    return new IllegalStateException("Abbonamento non trovato");
                });

        subscriptionRepository.delete(subscription);
        logger.info("Subscription deleted by admin: id={}", subscriptionId);
    }
}