package it.uniroma3.siwprogetto.service;

import it.uniroma3.siwprogetto.model.Dealer;
import it.uniroma3.siwprogetto.model.Image;
import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.repository.DealerRepository;
import it.uniroma3.siwprogetto.repository.ProductRepository;
import it.uniroma3.siwprogetto.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service layer per gestione concessionari automotive (Dealer) e prodotti associati
 * 
 * Responsabilità:
 * - Gestione CRUD completa dealer con ownership validation
 * - Gestione prodotti dealer con business rules automotive
 * - Featured products management con subscription integration
 * - Eliminazioni cascade complesse con cleanup dipendenze
 * - Upload e gestione immagini multipart
 * - Authorization security tramite Spring SecurityContext
 * 
 * Business Model:
 * - Dealer = concessionario automotive con catalogo prodotti
 * - Product DEALER type = venduto tramite concessionario
 * - Featured products = evidenziazione premium temporanea
 * - Owner relationship = User:Dealer one-to-one
 * 
 * Pattern Service vantaggi:
 * - Business logic centralizzata per dealer operations
 * - Security context integration per authorization
 * - Transaction management per operazioni complesse
 * - Native queries per performance critical operations
 * - Lazy loading optimization per relazioni
 */
@Service
public class DealerService {

    /**
     * Logger SLF4J per audit trail e debugging operazioni dealer
     * Tracciamento dettagliato per business operations critiche
     */
    private static final Logger logger = LoggerFactory.getLogger(DealerService.class);

    /**
     * Repository per accesso dati dealer
     * Gestione CRUD e query specializzate concessionari
     */
    @Autowired
    private DealerRepository dealerRepository;

    /**
     * Repository per accesso dati utenti
     * Authorization e ownership validation
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * Repository per accesso dati prodotti automotive
     * Gestione catalogo prodotti dealer
     */
    @Autowired
    private ProductRepository productRepository;

    /**
     * JPA EntityManager per native queries e flush operations
     * Performance optimization per eliminazioni cascade
     */
    @Autowired
    private EntityManager entityManager;

    /**
     * RestTemplate per API calls esterne (future use)
     * Integration con servizi terzi per validazioni
     */
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Salva o aggiorna dealer con validazione ownership e business rules
     * 
     * Business Logic:
     * - Un utente può avere al massimo UN dealer (one-to-one)
     * - Update mode: modifica dealer esistente dello stesso owner
     * - Create mode: crea nuovo dealer se non esiste per utente
     * - Security context validation per authenticated user
     * - Gestione immagini con relazione bidirezionale
     * 
     * @param dealer Oggetto dealer da salvare/aggiornare
     * @param isUpdate Flag modalità operazione (true=update, false=create)
     * @return Dealer persistito con ID generato/aggiornato
     * @throws IllegalStateException Se utente non autenticato o dealer già esistente
     * 
     * Authorization Rules:
     * - Solo utenti autenticati possono creare/modificare dealer
     * - Utente può modificare solo il proprio dealer
     * - Anonymous user = exception immediate
     * 
     * Transaction Scope:
     * - Create: dealer + images insert atomico
     * - Update: merge fields + images replace atomico
     * - Rollback automatico su eccezioni
     * 
     * Images Handling:
     * - Bidirezional mapping dealer <-> images
     * - Clear/replace strategy per updates
     * - Cascade persist per nuove immagini
     */
    @Transactional
    public Dealer saveDealer(Dealer dealer, boolean isUpdate) {
        logger.info("Saving dealer: name={}, isUpdate={}", dealer.getName(), isUpdate);
        
        // Security context validation
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.debug("Current authenticated user: {}", username);

        if (username == null || "anonymousUser".equals(username)) {
            logger.error("No authenticated user found");
            throw new IllegalStateException("Utente non autenticato");
        }

        // User loading con exception handling
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("Utente non trovato: {}", username);
                    return new IllegalStateException("Utente non trovato: " + username);
                });

        // Existing dealer check per business rule validation
        Optional<Dealer> existingDealer = dealerRepository.findByOwnerUsername(username);
        
        if (isUpdate && existingDealer.isPresent()) {
            // UPDATE MODE: modifica dealer esistente
            logger.debug("Updating existing dealer: id={}", existingDealer.get().getId());
            Dealer toUpdate = existingDealer.get();
            
            // Field mapping per update
            toUpdate.setName(dealer.getName());
            toUpdate.setDescription(dealer.getDescription());
            toUpdate.setAddress(dealer.getAddress());
            toUpdate.setPhone(dealer.getPhone());
            toUpdate.setEmail(dealer.getEmail());
            
            // Images replace strategy
            if (dealer.getImages() != null && !dealer.getImages().isEmpty()) {
                toUpdate.getImages().clear(); // Remove existing
                dealer.getImages().forEach(img -> {
                    img.setDealer(toUpdate); // Set bidirectional reference
                    toUpdate.getImages().add(img); // Add to collection
                });
            }
            
            Dealer savedDealer = dealerRepository.save(toUpdate);
            logger.info("Dealer updated: id={}, name={}", savedDealer.getId(), savedDealer.getName());
            return savedDealer;
            
        } else if (!isUpdate && existingDealer.isPresent()) {
            // CREATE MODE ERROR: dealer già esistente
            logger.error("Dealer already exists for user '{}': id={}", username, existingDealer.get().getId());
            throw new IllegalStateException("Un concessionario esiste già per questo utente");
            
        } else if (!isUpdate && !existingDealer.isPresent()) {
            // CREATE MODE: nuovo dealer
            logger.debug("Creating new dealer for user: id={}, username={}", user.getId(), username);
            dealer.setOwner(user); // Set ownership
            dealer.getImages().forEach(img -> img.setDealer(dealer)); // Bidirectional mapping
            
            Dealer savedDealer = dealerRepository.save(dealer);
            logger.info("Dealer created: id={}, name={}, owner_id={}", 
                    savedDealer.getId(), savedDealer.getName(), savedDealer.getOwner().getId());
            return savedDealer;
            
        } else {
            // UPDATE MODE ERROR: nessun dealer da aggiornare
            logger.error("Invalid state: isUpdate=true but no dealer exists for user '{}'", username);
            throw new IllegalStateException("Nessun concessionario trovato per la modifica");
        }
    }

    /**
     * Trova dealer dell'utente corrente autenticato
     * 
     * Authorization Strategy:
     * - Security context per username corrente
     * - Query by owner username per performance
     * - Lazy loading initialization per immagini
     * 
     * @return Dealer dell'utente corrente, null se non esiste o non autenticato
     * 
     * Performance Optimization:
     * - Read-only transaction per query efficiency  
     * - Hibernate.initialize() per eager loading immagini
     * - Single query con join automatico
     * 
     * Use Cases:
     * - Dashboard dealer per visualizzazione profilo
     * - Authorization check prima operazioni dealer
     * - Profile editing form population
     */
    @Transactional(readOnly = true)
    public Dealer findByOwner() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.debug("Finding dealer for user: {}", username);

        if (username == null || "anonymousUser".equals(username)) {
            logger.warn("No authenticated user found");
            return null;
        }

        Optional<Dealer> dealer = dealerRepository.findByOwnerUsername(username);
        if (dealer.isPresent()) {
            Hibernate.initialize(dealer.get().getImages()); // Eager load images
            return dealer.get();
        } else {
            logger.warn("No dealer found for user '{}'", username);
            return null;
        }
    }

    /**
     * Aggiunge prodotto al catalogo dealer con validazioni business
     * 
     * Business Rules:
     * - Solo dealer autenticati possono aggiungere prodotti
     * - Prodotti automaticamente marcati come "DEALER" type
     * - Seller = User owner del dealer (not dealer entity)
     * - Immagini prodotto con relazione bidirezionale
     * 
     * @param product Prodotto da aggiungere al catalogo
     * @return Product persistito con ID generato
     * @throws IllegalStateException Se utente non autenticato o non ha dealer
     * 
     * Authorization Chain:
     * 1. Security context validation (authenticated user)
     * 2. User existence check in database
     * 3. Dealer existence check per user corrente
     * 4. Product creation con seller = user
     * 
     * Data Mapping:
     * - seller = User owner (not Dealer entity)
     * - sellerType = "DEALER" (vs "PRIVATE")
     * - images bidirectional mapping
     * 
     * Transaction Atomicity:
     * - Product + Images insert in single transaction
     * - Rollback completo su qualsiasi failure
     */
    @Transactional
    public Product addProduct(Product product) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.debug("Adding product for user: {}", username);

        // Security validation
        if (username == null || "anonymousUser".equals(username)) {
            logger.error("No authenticated user found");
            throw new IllegalStateException("Utente non autenticato");
        }

        // User loading
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("Utente non trovato: {}", username);
                    return new IllegalStateException("Utente non trovato: " + username);
                });

        // Dealer existence validation
        Dealer dealer = findByOwner();
        if (dealer == null) {
            logger.error("No dealer found for user '{}'", username);
            throw new IllegalStateException("Nessun concessionario trovato");
        }

        // Product configuration per dealer business rules
        product.setSeller(user); // Seller = User (not Dealer)
        product.setSellerType("DEALER"); // Mark as dealer product
        product.getImages().forEach(img -> img.setProduct(product)); // Bidirectional mapping
        
        Product savedProduct = productRepository.save(product);
        logger.info("Product added: id={}, model={}, seller_id={}", 
                savedProduct.getId(), savedProduct.getModel(), user.getId());
        return savedProduct;
    }

    /**
     * Aggiorna prodotto esistente con validazione ownership
     * 
     * Authorization Rules:
     * - Solo dealer owner può modificare i propri prodotti
     * - Verifica product.seller.id = user.id corrente
     * - Exception per tentativi non autorizzati
     * 
     * @param productId ID del prodotto da modificare
     * @param updatedProduct Oggetto con nuovi valori
     * @return Product aggiornato e persistito
     * @throws IllegalStateException Se non autorizzato o prodotto non trovato
     * 
     * Updatable Fields:
     * - model, brand, category (identificazione)
     * - description, price (marketing)
     * - mileage, year (specifiche tecniche)
     * - fuelType, transmission (motorizzazione)
     * - images (portfolio visuale)
     * - isFeatured, featuredUntil (evidenziazione)
     * 
     * Immutable Fields:
     * - seller, sellerType (business logic)
     * - id, creationDate (system managed)
     * 
     * Images Strategy:
     * - Clear existing + replace con nuove
     * - Bidirectional mapping maintenance
     * - Cascade delete per immagini rimosse
     */
    @Transactional
    public Product updateProduct(Long productId, Product updatedProduct) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.debug("Updating product: id={}, user={}", productId, username);

        // Security validation
        if (username == null || "anonymousUser".equals(username)) {
            logger.error("No authenticated user found");
            throw new IllegalStateException("Utente non autenticato");
        }

        // User loading
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("Utente non trovato: {}", username);
                    return new IllegalStateException("Utente non trovato: " + username);
                });

        // Dealer existence validation
        Dealer dealer = findByOwner();
        if (dealer == null) {
            logger.error("No dealer found for user '{}'", username);
            throw new IllegalStateException("Nessun concessionario trovato");
        }

        // Product loading
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    logger.error("Product not found: id={}", productId);
                    return new IllegalStateException("Prodotto non trovato");
                });

        // Ownership validation
        if (!product.getSeller().getId().equals(user.getId())) {
            logger.error("Product does not belong to user: product_id={}, user_id={}", productId, user.getId());
            throw new IllegalStateException("Il prodotto non appartiene a questo utente");
        }

        // Field updates
        product.setModel(updatedProduct.getModel());
        product.setBrand(updatedProduct.getBrand());
        product.setCategory(updatedProduct.getCategory());
        product.setDescription(updatedProduct.getDescription());
        product.setPrice(updatedProduct.getPrice());
        product.setMileage(updatedProduct.getMileage());
        product.setYear(updatedProduct.getYear());
        product.setFuelType(updatedProduct.getFuelType());
        product.setTransmission(updatedProduct.getTransmission());
        
        // Images replace strategy
        if (updatedProduct.getImages() != null && !updatedProduct.getImages().isEmpty()) {
            product.getImages().clear(); // Remove existing
            updatedProduct.getImages().forEach(img -> {
                img.setProduct(product); // Bidirectional mapping
                product.getImages().add(img); // Add to collection
            });
        }
        
        // Featured product settings
        product.setIsFeatured(updatedProduct.isFeatured());
        product.setFeaturedUntil(updatedProduct.getFeaturedUntil());
        
        Product savedProduct = productRepository.save(product);
        logger.info("Product updated: id={}, model={}, seller_id={}", 
                savedProduct.getId(), savedProduct.getModel(), user.getId());
        return savedProduct;
    }

    /**
     * Recupera tutti i prodotti del dealer corrente autenticato
     * 
     * @return Lista prodotti del dealer (può essere vuota)
     * 
     * Performance Optimization:
     * - Read-only transaction per efficiency
     * - Hibernate.initialize() per eager loading immagini
     * - Single query con join implicito
     * 
     * Authorization:
     * - Solo prodotti del dealer corrente
     * - No access a prodotti di altri dealer
     * - Empty list se dealer non esiste
     * 
     * Use Cases:
     * - Dashboard dealer inventory management
     * - Catalogo prodotti dealer specifico
     * - Statistics e analytics per dealer
     */
    @Transactional(readOnly = true)
    public List<Product> getProductsByDealer() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.debug("Retrieving products for user: {}", username);

        if (username == null || "anonymousUser".equals(username)) {
            logger.warn("No authenticated user found");
            return List.of(); // Empty list per anonymous users
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("Utente non trovato: {}", username);
                    return new IllegalStateException("Utente non trovato: " + username);
                });

        Dealer dealer = findByOwner();
        if (dealer == null) {
            logger.warn("No dealer found for user '{}'", username);
            return List.of(); // Empty list se dealer non esiste
        }

        // Prodotti del seller (user) con eager loading images
        List<Product> products = productRepository.findBySellerId(user.getId());
        products.forEach(product -> Hibernate.initialize(product.getImages()));
        logger.info("Found {} products for user: id={}", products.size(), user.getId());
        return products;
    }

    /**
     * Cerca dealer per localizzazione geografica
     * 
     * @param query Stringa di ricerca per indirizzo (case-insensitive)
     * @return Lista dealer che matchano la query location
     * 
     * Search Strategy:
     * - Case-insensitive LIKE query su address field
     * - Full text search su indirizzo completo
     * - Empty/null query = return all dealers
     * 
     * Use Cases:
     * - Ricerca dealer per zona geografica
     * - Map integration per localizzazione
     * - Customer finder per area specifica
     */
    @Transactional(readOnly = true)
    public List<Dealer> findByLocation(String query) {
        logger.debug("Finding dealers with query: {}", query);
        if (query == null || query.trim().isEmpty()) {
            return dealerRepository.findAll(); // All dealers se query vuota
        }
        return dealerRepository.findByAddressContainingIgnoreCase(query);
    }

    /**
     * Recupera tutti i dealer del sistema
     * 
     * @return Lista completa dealer con immagini
     * 
     * Performance:
     * - Read-only transaction
     * - Eager loading immagini per tutti i dealer
     * - Potentially expensive per grandi dataset
     * 
     * Use Cases:
     * - Dashboard amministrativa
     * - Map view con tutti i dealer
     * - Export dati per analytics
     */
    @Transactional(readOnly = true)
    public List<Dealer> findAll() {
        logger.info("Retrieving all dealers");
        List<Dealer> dealers = dealerRepository.findAll();
        dealers.forEach(dealer -> Hibernate.initialize(dealer.getImages())); // Eager load images
        logger.info("Trovati {} concessionari.", dealers.size());
        return dealers;
    }

    /**
     * Trova prodotto per ID con eager loading immagini
     * 
     * @param id ID del prodotto da cercare
     * @return Product se trovato, null se non esiste
     * 
     * Performance:
     * - Read-only transaction
     * - Hibernate.initialize() per immagini
     * - Single query optimization
     * 
     * Use Cases:
     * - Dettaglio prodotto con gallery immagini
     * - Product page loading
     * - Edit product form population
     */
    @Transactional(readOnly = true)
    public Product findProductById(Long id) {
        logger.debug("Finding product by ID: {}", id);
        Optional<Product> product = productRepository.findById(id);
        if (product.isPresent()) {
            Hibernate.initialize(product.get().getImages()); // Eager load images
            return product.get();
        } else {
            logger.error("Product not found: id={}", id);
            return null;
        }
    }

    /**
     * Elimina prodotto con cascade cleanup quote requests
     * 
     * ATTENZIONE: Operazione irreversibile con side effects
     * - Eliminazione quote_requests associate a TUTTI i prodotti seller
     * - Native query per performance su large datasets
     * - Physical delete dal database (no soft delete)
     * 
     * @param id ID del prodotto da eliminare
     * @throws IllegalStateException Se prodotto non trovato
     * @throws RuntimeException Se errore durante eliminazione
     * 
     * Deletion Strategy Multi-Step:
     * 1. Product loading con validation
     * 2. Quote requests cleanup via native query (ALL seller products)
     * 3. EntityManager flush per commit intermediate
     * 4. Product delete via repository
     * 5. Exception handling con rollback automatico
     * 
     * Performance Consideration:
     * - Native query per bulk delete efficiente
     * - EntityManager flush per consistency
     * - Transaction rollback su qualsiasi errore
     * 
     * Business Impact:
     * - Quote requests loss per tutti i prodotti seller
     * - Conversation history persa definitivamente
     * - Audit trail necessario per compliance
     */
    @Transactional
    public void deleteProduct(Long id) {
        logger.debug("Deleting product: id={}", id);
        
        try {
            // Product loading con validation
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> {
                        logger.error("Product not found: id={}", id);
                        return new IllegalStateException("Prodotto non trovato");
                    });

            // Step 1: Elimina TUTTE le quote_requests associate ai prodotti del seller
            try {
                Query deleteAllQuoteRequests = entityManager.createNativeQuery(
                    "DELETE FROM quote_requests WHERE product_id IN (SELECT id FROM product WHERE seller_id = ?)");
                deleteAllQuoteRequests.setParameter(1, product.getSeller().getId());
                int deletedQuotes = deleteAllQuoteRequests.executeUpdate();
                logger.debug("Deleted {} quote requests for all products", deletedQuotes);
                
                // Flush per commit intermediate results
                entityManager.flush();
            } catch (Exception e) {
                logger.warn("Could not delete quote_requests: {}", e.getMessage());
                // Continue execution, table might not exist
            }

            // Step 2: Flush per assicurarsi che le quote_requests siano eliminate
            entityManager.flush();

            // Step 3: Elimina il prodotto
            productRepository.delete(product);
            logger.info("Product deleted: id={}", id);
            
        } catch (Exception e) {
            logger.error("Error deleting product: id={}, error={}", id, e.getMessage());
            throw new RuntimeException("Errore durante l'eliminazione del prodotto: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina dealer con cascade cleanup completo di tutte le dipendenze
     * 
     * ATTENZIONE: Operazione complessa e irreversibile con multiple dependencies
     * - Eliminazione quote_requests per tutti i prodotti dealer
     * - Eliminazione prodotti associati via delegation
     * - Eliminazione immagini dealer
     * - Eliminazione dealer con native query per reliability
     * - Verification post-eliminazione per consistency
     * 
     * @param id ID del dealer da eliminare
     * @throws IllegalArgumentException Se ID non valido o dealer non trovato
     * @throws RuntimeException Se errore durante eliminazione
     * 
     * Deletion Strategy Multi-Phase:
     * 1. Input validation e dealer loading
     * 2. Quote requests bulk delete (native query)
     * 3. Products individual delete via service delegation
     * 4. Dealer images cleanup (native query)
     * 5. EntityManager flush per intermediate consistency
     * 6. Dealer delete via native query (reliability)
     * 7. Post-deletion verification per success confirmation
     * 
     * Native Queries Rationale:
     * - Performance su large datasets
     * - Bypass JPA cascade constraints issues
     * - Direct SQL control per complex deletions
     * - Reliability per critical operations
     * 
     * Error Handling Strategy:
     * - Partial failure tolerance (warn + continue)
     * - Final verification per operation success
     * - Exception chain per detailed error reporting
     * - Transaction rollback automatico su RuntimeException
     * 
     * Business Impact:
     * - Dealer profile permanently deleted
     * - All dealer products removed from catalog
     * - Quote requests conversations lost
     * - User account mantained (no cascade to User)
     */
    @Transactional
    public void deleteDealer(Long id) {
        logger.info("Attempting to delete dealer with ID: {}", id);

        // Input validation
        if (id == null || id <= 0) {
            logger.error("Invalid dealer ID: {}", id);
            throw new IllegalArgumentException("ID del concessionario non valido.");
        }

        try {
            // Dealer loading con validation
            Dealer dealer = dealerRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Dealer not found: {}", id);
                    return new IllegalArgumentException("Concessionario non trovato con ID: " + id);
                });

            logger.debug("Dealer found: id={}, name={}, owner={}", 
                dealer.getId(), dealer.getName(), dealer.getOwner().getUsername());

            // Step 1: Elimina quote_requests se esistono (native query for performance)
            try {
                Query deleteQuoteRequests = entityManager.createNativeQuery(
                    "DELETE FROM quote_requests WHERE product_id IN (SELECT id FROM product WHERE seller_id = ?)");
                deleteQuoteRequests.setParameter(1, dealer.getOwner().getId());
                int deletedQuotes = deleteQuoteRequests.executeUpdate();
                logger.debug("Deleted {} quote requests", deletedQuotes);
            } catch (Exception e) {
                logger.warn("Could not delete quote_requests (table might not exist): {}", e.getMessage());
                // Continue execution, table might not exist
            }

            // Step 2: Elimina prodotti via service delegation (business logic preservation)
            List<Product> products = productRepository.findBySellerId(dealer.getOwner().getId());
            for (Product product : products) {
                try {
                    deleteProduct(product.getId()); // Delegation to service method
                    logger.debug("Deleted product: {}", product.getId());
                } catch (Exception e) {
                    logger.warn("Could not delete product {}: {}", product.getId(), e.getMessage());
                    // Continue with other products
                }
            }

            // Step 3: Elimina immagini dealer (native query for performance)
            try {
                Query deleteDealerImages = entityManager.createNativeQuery("DELETE FROM image WHERE dealer_id = ?");
                deleteDealerImages.setParameter(1, id);
                int deletedImages = deleteDealerImages.executeUpdate();
                logger.debug("Deleted {} dealer images", deletedImages);
            } catch (Exception e) {
                logger.warn("Could not delete dealer images: {}", e.getMessage());
                // Continue execution
            }

            // Step 4: Flush per intermediate consistency
            entityManager.flush();

            // Step 5: Elimina dealer con native query (reliability over JPA)
            logger.info("Deleting dealer with native query: {}", id);
            Query deleteDealerQuery = entityManager.createNativeQuery("DELETE FROM dealer WHERE id = ?");
            deleteDealerQuery.setParameter(1, id);
            int deletedRows = deleteDealerQuery.executeUpdate();
            logger.info("Dealer deletion query executed: {} rows affected", deletedRows);

            // Step 6: Final flush
            entityManager.flush();

            // Step 7: Post-deletion verification
            boolean dealerExists = dealerRepository.existsById(id);
            if (dealerExists) {
                logger.error("FALLIMENTO: Il dealer esiste ancora dopo l'eliminazione: {}", id);
                throw new RuntimeException("Errore: il concessionario non è stato eliminato correttamente");
            }

            logger.info("SUCCESS: Dealer with ID: {} deleted successfully and verified", id);

        } catch (Exception e) {
            logger.error("Error deleting dealer ID: {}", id, e);
            throw new RuntimeException("Errore durante l'eliminazione del concessionario: " + e.getMessage(), e);
        }
    }

    /**
     * Trova dealer per username owner
     * 
     * @param username Username del proprietario dealer
     * @return Dealer se trovato, null se non esiste o username non valido
     * 
     * Validation:
     * - Input sanitization per username
     * - Empty/null handling
     * - Logging dettagliato per troubleshooting
     * 
     * Performance:
     * - Read-only transaction
     * - Eager loading immagini
     * - Single query optimization
     * 
     * Use Cases:
     * - Authorization checks
     * - Profile loading per username
     * - Admin operations su dealer specifico
     */
    @Transactional(readOnly = true)
    public Dealer findByOwnerUsername(String username) {
        logger.debug("Finding dealer by owner username: {}", username);

        // Input validation
        if (username == null || username.trim().isEmpty()) {
            logger.warn("Invalid username provided: {}", username);
            return null;
        }

        Optional<Dealer> dealer = dealerRepository.findByOwnerUsername(username);
        if (dealer.isPresent()) {
            logger.info("Dealer found for username '{}': id={}, name={}", 
                username, dealer.get().getId(), dealer.get().getName());
            Hibernate.initialize(dealer.get().getImages()); // Eager load images
            return dealer.get();
        } else {
            logger.warn("No dealer found for username '{}'", username);
            return null;
        }
    }

    /**
     * Evidenzia prodotto per durata specificata (featured product)
     * 
     * Business Rules:
     * - Solo dealer owner può evidenziare i propri prodotti
     * - Durata in giorni deve essere positiva
     * - Featured fino data = now + duration days
     * - isFeatured flag = true per visibility
     * 
     * @param productId ID del prodotto da evidenziare
     * @param duration Durata evidenziazione in giorni
     * @return Product con evidenziazione applicata
     * @throws IllegalStateException Se non autorizzato o durata non valida
     * 
     * Authorization Chain:
     * 1. Security context validation
     * 2. User loading e dealer existence
     * 3. Product loading e ownership verification
     * 4. Duration validation business rule
     * 
     * Featured Logic:
     * - isFeatured = true (visibility flag)
     * - featuredUntil = LocalDateTime.now() + duration days
     * - Auto-expiry gestita da business logic queries
     * 
     * Subscription Integration:
     * - Future enhancement: check user subscription limits
     * - Premium users = unlimited featured products
     * - Basic users = limited featured slots
     */
    @Transactional
    public Product highlightProduct(Long productId, int duration) {
        logger.debug("Highlighting product: id={}, duration={}", productId, duration);

        // Security validation
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (username == null || "anonymousUser".equals(username)) {
            logger.error("No authenticated user found");
            throw new IllegalStateException("Utente non autenticato");
        }

        // User loading
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("Utente non trovato: {}", username);
                    return new IllegalStateException("Utente non trovato: " + username);
                });

        // Dealer existence validation
        Dealer dealer = findByOwner();
        if (dealer == null) {
            logger.error("No dealer found for user '{}'", username);
            throw new IllegalStateException("Nessun concessionario trovato");
        }

        // Product loading
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    logger.error("Product not found: id={}", productId);
                    return new IllegalStateException("Prodotto non trovato");
                });

        // Ownership validation
        if (!product.getSeller().getId().equals(user.getId())) {
            logger.error("Product does not belong to user: product_id={}, user_id={}", productId, user.getId());
            throw new IllegalStateException("Il prodotto non appartiene a questo utente");
        }

        // Duration validation
        if (duration <= 0) {
            logger.error("Invalid highlight duration: {}", duration);
            throw new IllegalStateException("La durata dell'evidenza deve essere positiva");
        }

        // Featured product configuration
        product.setIsFeatured(true);
        product.setFeaturedUntil(LocalDateTime.now().plusDays(duration));
        
        Product savedProduct = productRepository.save(product);
        logger.info("Product highlighted: id={}, model={}, featuredUntil={}", 
                savedProduct.getId(), savedProduct.getModel(), savedProduct.getFeaturedUntil());
        return savedProduct;
    }

    /**
     * Rimuove evidenziazione da prodotto (remove highlight)
     * 
     * @param productId ID del prodotto da cui rimuovere evidenziazione
     * @return Product con evidenziazione rimossa
     * @throws IllegalStateException Se non autorizzato o prodotto non trovato
     * 
     * Authorization:
     * - Same authorization chain as highlightProduct
     * - Owner-only operation per security
     * 
     * Highlight Removal:
     * - isFeatured = false (hide from featured lists)
     * - featuredUntil = null (clear expiry date)
     * - Immediate effect on queries
     * 
     * Use Cases:
     * - Manual highlight removal prima scadenza
     * - Inventory management per dealer
     * - Cost saving per subscription limits
     */
    @Transactional
    public Product removeHighlight(Long productId) {
        logger.debug("Removing highlight from product: id={}", productId);

        // Security validation (same as highlightProduct)
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (username == null || "anonymousUser".equals(username)) {
            logger.error("No authenticated user found");
            throw new IllegalStateException("Utente non autenticato");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("Utente non trovato: {}", username);
                    return new IllegalStateException("Utente non trovato: " + username);
                });

        Dealer dealer = findByOwner();
        if (dealer == null) {
            logger.error("No dealer found for user '{}'", username);
            throw new IllegalStateException("Nessun concessionario trovato");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    logger.error("Product not found: id={}", productId);
                    return new IllegalStateException("Prodotto non trovato");
                });

        // Ownership validation
        if (!product.getSeller().getId().equals(user.getId())) {
            logger.error("Product does not belong to user: product_id={}, user_id={}", productId, user.getId());
            throw new IllegalStateException("Il prodotto non appartiene a questo utente");
        }

        // Remove highlight configuration
        product.setIsFeatured(false);
        product.setFeaturedUntil(null);
        
        Product savedProduct = productRepository.save(product);
        logger.info("Highlight removed from product: id={}, model={}", 
                savedProduct.getId(), savedProduct.getModel());
        return savedProduct;
    }

    /**
     * Trova dealer per ID con eager loading immagini
     * 
     * @param id ID del dealer da cercare
     * @return Dealer se trovato, null se non esiste
     * 
     * Use Cases:
     * - Dealer profile page loading
     * - Admin operations su dealer specifico
     * - Authorization checks pre-operations
     */
    @Transactional(readOnly = true)
    public Dealer findById(Long id) {
        logger.debug("Finding dealer by ID: {}", id);
        Optional<Dealer> dealer = dealerRepository.findById(id);
        if (dealer.isPresent()) {
            Hibernate.initialize(dealer.get().getImages()); // Eager load images
            return dealer.get();
        } else {
            logger.warn("No dealer found for ID '{}'", id);
            return null;
        }
    }

    /**
     * Recupera prodotti di un dealer specifico tramite owner
     * 
     * @param dealer Dealer per cui cercare i prodotti
     * @return Lista prodotti del dealer con immagini
     * @throws IllegalArgumentException Se dealer o owner non validi
     * 
     * Validation:
     * - Dealer non null
     * - Owner associato al dealer
     * - Business logic consistency
     * 
     * Performance:
     * - Eager loading immagini per tutti i prodotti
     * - Single query per dealer owner products
     * 
     * Use Cases:
     * - Dealer profile con catalogo prodotti
     * - Admin view dealer inventory
     * - Analytics per dealer specifico
     */
    @Transactional(readOnly = true)
    public List<Product> getProductsByDealerOwner(Dealer dealer) {
        // Input validation
        if (dealer == null) {
            throw new IllegalArgumentException("Concessionario non valido.");
        }
        if (dealer.getOwner() == null) {
            throw new IllegalStateException("Il concessionario non ha un proprietario associato.");
        }
        
        List<Product> products = productRepository.findBySeller(dealer.getOwner());
        products.forEach(product -> Hibernate.initialize(product.getImages())); // Eager load images
        return products;
    }

    /**
     * Prepara oggetto Image da MultipartFile per persistenza
     * 
     * @param file File immagine multipart da request HTTP
     * @return Image entity pronta per persistenza
     * @throws IOException Se errore lettura file
     * @throws IllegalArgumentException Se file non valido
     * 
     * File Processing:
     * - Byte array conversion da MultipartFile
     * - ContentType preservation per rendering corretto
     * - Input validation per file non vuoti
     * 
     * Use Cases:
     * - Upload immagini dealer profile
     * - Upload immagini prodotto
     * - Form submission processing
     * 
     * Security Considerations:
     * - File size limits gestiti a controller level
     * - MIME type validation per security
     * - Virus scanning integration future enhancement
     */
    @Transactional
    public Image saveImageFile(MultipartFile file) throws IOException {
        // Input validation
        if (file == null || file.isEmpty()) {
            logger.warn("File immagine non valido o vuoto");
            throw new IllegalArgumentException("File immagine non valido");
        }

        // Image entity preparation
        Image image = new Image();
        image.setData(file.getBytes()); // Binary data conversion
        image.setContentType(file.getContentType()); // MIME type preservation
        
        logger.info("Immagine preparata per il salvataggio: size={} bytes, contentType={}", 
                file.getBytes().length, file.getContentType());
        return image;
    }

    /**
     * Recupera utente corrente autenticato dal Security Context
     * 
     * @return User autenticato correntemente
     * @throws IllegalStateException Se utente non autenticato o non trovato
     * 
     * Security Integration:
     * - Spring Security Context access
     * - Username extraction da Authentication
     * - Database lookup per User entity completo
     * 
     * Use Cases:
     * - Authorization operations
     * - User-specific business logic
     * - Audit trail con user identification
     * 
     * Performance:
     * - Read-only transaction
     * - Cached security context access
     * - Single query per username
     */
    @Transactional(readOnly = true)
    public User getAuthenticatedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
              return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Utente autenticato non trovato: " + username));
    }
}