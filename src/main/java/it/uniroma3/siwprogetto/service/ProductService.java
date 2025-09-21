package it.uniroma3.siwprogetto.service;

import it.uniroma3.siwprogetto.model.Image;
import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.model.Subscription;
import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.repository.ProductRepository;
import it.uniroma3.siwprogetto.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer per gestione business logic dei prodotti automotive
 * 
 * Responsabilità:
 * - Operazioni CRUD su prodotti con validazioni business
 * - Gestione ricerca e filtri avanzati catalogo
 * - Logic per prodotti in evidenza e limiti subscription
 * - Elaborazione immagini prodotto
 * - Coordinamento transazioni complesse
 * 
 * Pattern Service vantaggi:
 * - Separazione business logic dal controller
 * - Transazioni dichiarative con @Transactional
 * - Logging strutturato per debugging e audit
 * - Gestione eccezioni centralizzata
 * - Riusabilità logic tra diversi controller
 * 
 * Architettura:
 * - Service layer intermedio tra Controller e Repository
 * - Gestione state consistency tramite transazioni
 * - Integration con altri services per operazioni complesse
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
@Service
public class ProductService {

    /**
     * Repository per accesso dati prodotti
     * Iniettato tramite @Autowired per dependency injection
     */
    @Autowired
    private ProductRepository productRepository;

    /**
     * Repository per accesso dati utenti
     * Necessario per caricare subscription completa utente
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * Logger SLF4J per tracciamento operazioni e debugging
     * Configurazione centralizzata via logback/log4j
     */
    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    /**
     * Costruttore per dependency injection manuale se necessario
     * Compatibilità con test unitari e configurazioni custom
     * 
     * @param productRepository Repository prodotti da iniettare
     */
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Recupera tutti i prodotti presenti nel catalogo
     * 
     * @Transactional garantisce:
     * - Consistenza lettura durante transazione
     * - Lazy loading automatico relazioni
     * - Rollback automatico su eccezioni runtime
     * 
     * @return Lista completa prodotti (può essere vuota)
     * 
     * Utilizzi:
     * - Caricamento catalogo homepage
     * - Dashboard amministrativa
     * - Export dati per analytics
     * 
     * Performance:
     * - Query potenzialmente costosa per grandi cataloghi
     * - Considerare paginazione per dataset estesi
     */
    @Transactional
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    /**
     * Trova prodotti filtrati per categoria specifica
     * 
     * @param category Categoria da cercare (es: "AUTO", "MOTO", "CAMPER")
     * @return Lista prodotti della categoria specificata
     * 
     * Business Logic:
     * - Delegata completamente al repository layer
     * - Nessuna validazione business aggiuntiva richiesta
     * - Transazione read-only per performance
     */
    @Transactional
    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    /**
     * Recupera tutte le categorie distinte presenti nel catalogo
     * 
     * @return Lista categorie univoche per popolamento filtri UI
     * 
     * Cache Strategy:
     * - Risultato relativamente stabile nel tempo
     * - Candidato ideale per caching applicativo
     * - Invalidazione cache su inserimento nuove categorie
     */
    @Transactional
    public List<String> findAllCategories() {
        return productRepository.findAllCategories();
    }

    /**
     * Recupera tutti i brand distinti dal catalogo
     * 
     * NOTA: Implementazione alternativa a repository.findAllBrands()
     * - Carica tutti i prodotti in memoria
     * - Estrae brand via Stream API
     * - Meno efficiente della query DISTINCT nativa
     * 
     * @return Lista brand univoci estratti da tutti i prodotti
     * 
     * Performance Impact:
     * - Caricamento completo catalogo in memoria
     * - Processing Stream su dataset potenzialmente grande
     * - Preferire repository.findAllBrands() per performance
     * 
     * Motivo implementazione:
     * - Possibile requirement di processing aggiuntivo sui dati
     * - Debugging o validazione brand format
     */
    @Transactional
    public List<String> findAllBrands() {
        return productRepository.findAll().stream()
                .map(Product::getBrand)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Trova modelli disponibili per brand specifico
     * 
     * @param brand Marca per cui cercare modelli disponibili
     * @return Lista modelli univoci del brand
     * 
     * UX Pattern:
     * - Cascading dropdown (prima brand, poi modello)
     * - Riduce opzioni utente progressivamente
     * - Migliora usabilità ricerca guidata
     */
    @Transactional
    public List<String> findModelsByBrand(String brand) {
        return productRepository.findModelsByBrand(brand);
    }

    /**
     * Recupera tipi carburante distinti nel catalogo
     * 
     * @return Lista tipi alimentazione per filtri ricerca
     * 
     * Business Domain:
     * - "BENZINA", "DIESEL", "ELETTRICO", "IBRIDO", etc.
     * - Importante per compliance normative ambientali
     * - Trend mercato verso soluzioni green
     */
    @Transactional
    public List<String> findAllFuelTypes() {
        return productRepository.findAllFuelTypes();
    }

    /**
     * Recupera tipi trasmissione distinti nel catalogo
     * 
     * @return Lista tipi cambio per filtri ricerca
     * 
     * Tipologie comuni:
     * - "MANUALE", "AUTOMATICO", "SEMIAUTOMATICO"
     * - CVT, DSG e altre varianti tecniche
     * - Preferenze utente driving experience
     */
    @Transactional
    public List<String> findAllTransmissions() {
        return productRepository.findAllTransmissions();
    }

    /**
     * Ricerca avanzata con filtri multipli e ricerca testuale
     * 
     * Processo in due fasi:
     * 1. Filtri database via repository (performance)
     * 2. Filtro testuale in-memory via Stream API (flessibilità)
     * 
     * @param category Categoria prodotto (opzionale)
     * @param brand Marca veicolo (opzionale)
     * @param selectedModel Modello specifico (opzionale)
     * @param minPrice Prezzo minimo inclusivo (opzionale)
     * @param maxPrice Prezzo massimo inclusivo (opzionale)
     * @param minMileage Chilometraggio minimo (opzionale)
     * @param maxMileage Chilometraggio massimo (opzionale)
     * @param minYear Anno minimo (opzionale)
     * @param maxYear Anno massimo (opzionale)
     * @param fuelType Tipo carburante (opzionale)
     * @param transmission Tipo trasmissione (opzionale)
     * @param query Testo libero per ricerca (opzionale)
     * @return Lista prodotti matching tutti i filtri
     * 
     * Algoritmo ricerca testuale:
     * - Split query per whitespace in termini separati
     * - Ogni termine deve matchare almeno un campo (AND logic)
     * - Match case-insensitive su brand, model, category
     * - Stream filtering per massima flessibilità
     * 
     * Logging Strategy:
     * - Debug level per parametri input
     * - Trace level per risultati intermedi
     * - Info level per metriche performance
     */
    @Transactional
    public List<Product> findByFilters(String category, String brand, String selectedModel,
                                       BigDecimal minPrice, BigDecimal maxPrice,
                                       Integer minMileage, Integer maxMileage,
                                       Integer minYear, Integer maxYear,
                                       String fuelType, String transmission, String query) {
        
        // Log parametri input per debugging
        logger.debug("Filtri applicati: category={}, brand={}, model={}, minPrice={}, maxPrice={}, " +
                     "minMileage={}, maxMileage={}, minYear={}, maxYear={}, fuelType={}, transmission={}, query={}",
                category, brand, selectedModel, minPrice, maxPrice, minMileage, maxMileage, 
                minYear, maxYear, fuelType, transmission, query);

        // Prima fase: filtri database per performance
        List<Product> results = productRepository.findByFilters(category, brand, selectedModel,
                minPrice, maxPrice, minMileage, maxMileage,
                minYear, maxYear, fuelType, transmission);

        // Seconda fase: filtro testuale in-memory se specificato
        if (query != null && !query.trim().isEmpty()) {
            // Preprocessing query: trim, lowercase, split per whitespace
            String[] searchTerms = query.trim().toLowerCase().split("\\s+");
            logger.debug("Termini di ricerca: {}", Arrays.toString(searchTerms));

            // Stream filtering con AND logic sui termini
            results = results.stream()
                    .filter(p -> {
                        // Ogni termine deve matchare almeno un campo
                        for (String term : searchTerms) {
                            boolean termMatches = (p.getBrand() != null && p.getBrand().toLowerCase().contains(term)) ||
                                    (p.getModel() != null && p.getModel().toLowerCase().contains(term)) ||
                                    (p.getCategory() != null && p.getCategory().toLowerCase().contains(term));
                            if (!termMatches) {
                                return false; // Termine non trovato, scarta prodotto
                            }
                        }
                        return true; // Tutti i termini matchano
                    })
                    .collect(Collectors.toList());
        }

        // Logging risultati per debugging e metriche
        logger.debug("Trovati {} prodotti", results.size());
        results.forEach(p -> logger.debug("Prodotto: id={}, brand={}, model={}", 
                        p.getId(), p.getBrand(), p.getModel()));
        return results;
    }

    /**
     * Trova prodotto per ID con gestione Optional
     * 
     * @param id ID prodotto da cercare
     * @return Optional contenente Product se trovato, empty() se non esiste
     * 
     * Pattern Optional:
     * - Null-safety garantita dal tipo di ritorno
     * - Forcing explicit null handling nel calling code
     * - Preferibile per API moderne e type-safety
     */
    @Transactional
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    /**
     * Trova prodotto per ID con ritorno diretto (legacy)
     * 
     * @param id ID prodotto da cercare
     * @return Product se trovato, null se non esiste
     * 
     * ATTENZIONE: Legacy method con possibile null return
     * - Richiede null check esplicito nel calling code
     * - Maintained per backward compatibility
     * - Preferire findById(Long) per nuove implementazioni
     */
    @Transactional
    public Product findById1(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    /**
     * Salva o aggiorna prodotto nel database
     * 
     * @param product Prodotto da salvare/aggiornare
     * @return Product salvato con ID assegnato se nuovo
     * 
     * JPA Behavior:
     * - Se ID null -> INSERT nuovo record
     * - Se ID esistente -> UPDATE record esistente
     * - Gestione automatica timestamp audit se configurato
     * - Cascade operations su relazioni annotate
     */
    @Transactional
    public Product save(Product product) {
        return productRepository.save(product);
    }

    /**
     * Elimina prodotto per ID
     * 
     * @param id ID prodotto da eliminare
     * 
     * ATTENZIONE: Eliminazione fisica irreversibile
     * - Verificare dipendenze (QuoteRequest, CartItem)
     * - Considerare soft delete per audit trail
     * - Transazione automatica per consistenza
     */
    @Transactional
    public void deleteById(Long id) {
        productRepository.deleteById(id);
    }

    /**
     * Trova prodotti di venditore specifico
     * 
     * @param sellerId ID venditore/utente proprietario
     * @return Lista prodotti del seller (può essere vuota)
     * 
     * Utilizzi:
     * - Dashboard venditore con propri annunci
     * - Gestione inventory personale
     * - Statistiche vendite per seller
     */
    @Transactional
    public List<Product> findBySellerId(Long sellerId) {
        return productRepository.findBySellerId(sellerId);
    }

    /**
     * Verifica se utente può aggiungere prodotto in evidenza
     * 
     * Business Logic complessa:
     * 1. Carica utente completo con subscription
     * 2. Verifica esistenza subscription attiva
     * 3. Recupera limite featured products per subscription
     * 4. Conta featured products attuali utente
     * 5. Esclude prodotto corrente dal conteggio se già featured
     * 6. Confronta con limite subscription
     * 
     * @param user Utente per cui verificare limite
     * @param product Prodotto da verificare (può essere null per nuovo)
     * @return true se utente può aggiungere/mantenere featured product
     * 
     * Exception Handling:
     * - IllegalStateException se utente non trovato
     * - Return false per subscription null/invalida
     * - Logging dettagliato per debugging business logic
     * 
     * Edge Cases:
     * - Prodotto già featured (escluso dal conteggio)
     * - maxFeaturedProducts <= 0 (subscription invalida)
     * - Subscription null o non attiva
     */
    @Transactional
    public boolean canAddFeaturedCar(User user, Product product) {
        // Carica utente completo con relazioni per evitare LazyInitializationException
        User fullUser = userRepository.findById(user.getId())
                .orElseThrow(() -> {
                    logger.error("Utente non trovato: {}", user.getId());
                    return new IllegalStateException("Utente non trovato");
                });

        // Recupera subscription attiva utente
        Subscription subscription = fullUser.getSubscription();
        if (subscription == null) {
            logger.warn("Nessun abbonamento trovato per l'utente: {}", fullUser.getUsername());
            return false;
        }

        // Verifica limite featured products per subscription
        int maxFeaturedProducts = subscription.getMaxFeaturedCars();
        logger.debug("Limite massimo di prodotti in evidenza per l'utente {}: {}", 
                     fullUser.getUsername(), maxFeaturedProducts);
        if (maxFeaturedProducts <= 0) {
            logger.warn("maxFeaturedProducts non valido per l'utente {}: {}", 
                        fullUser.getUsername(), maxFeaturedProducts);
            return false;
        }

        // Conta featured products attuali utente
        long featuredCount = productRepository.countBySellerAndIsFeaturedTrue(user);
        logger.debug("Prodotti attualmente in evidenza per l'utente {}: {}", 
                     fullUser.getUsername(), featuredCount);

        // Esclude prodotto corrente se già featured (caso update)
        if (product != null && product.isFeatured()) {
            featuredCount--;
            logger.debug("Escludendo il prodotto corrente, conteggio aggiornato: {}", featuredCount);
        }

        // Verifica limite subscription
        boolean canAdd = featuredCount < maxFeaturedProducts;
        logger.info("L'utente {} può aggiungere un prodotto in evidenza: {}", 
                    fullUser.getUsername(), canAdd);
        return canAdd;
    }

    /**
     * Imposta stato featured di un prodotto con validazioni business
     * 
     * Workflow:
     * 1. Carica prodotto dal database
     * 2. Estrae venditore/proprietario
     * 3. Verifica limiti subscription se featured=true
     * 4. Aggiorna stato e timestamp featured
     * 5. Persiste modifiche nel database
     * 
     * @param id ID prodotto da modificare
     * @param isFeatured Nuovo stato evidenziazione
     * @param featuredUntil Timestamp scadenza evidenziazione (può essere null)
     * 
     * @throws IllegalStateException Se prodotto non trovato o limite superato
     * 
     * Business Rules:
     * - Solo utenti con subscription attiva possono evidenziare
     * - Rispetto limiti maxFeaturedCars per piano subscription
     * - featuredUntil null = featured permanente
     * - featuredUntil passato = auto-disabilitazione via job schedulati
     */
    @Transactional
    public void setProductFeatured(Long id, boolean isFeatured, LocalDateTime featuredUntil) {
        // Carica prodotto con error handling
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Prodotto non trovato"));
        
        // Estrae venditore per verifica limiti
        User user = product.getSeller();
        
        // Verifica business constraints per featured=true
        if (isFeatured && !canAddFeaturedCar(user, product)) {
            throw new IllegalStateException("Limite massimo di prodotti in evidenza raggiunto per il tuo abbonamento");
        }
        
        // Applica modifiche al prodotto
        product.setIsFeatured(isFeatured);
        product.setFeaturedUntil(featuredUntil);
        
        // Persiste nel database
        productRepository.save(product);
    }

    /**
     * Elabora file immagine caricato e crea entità Image
     * 
     * Processing Steps:
     * 1. Validazione file input (null, empty checks)
     * 2. Estrazione byte array da MultipartFile
     * 3. Creazione entità Image con metadata
     * 4. Return Image pronta per associazione a Product
     * 
     * @param file MultipartFile caricato dall'utente
     * @return Image entity pronta per salvataggio
     * @throws IOException Se errore lettura file
     * @throws IllegalArgumentException Se file null/empty
     * 
     * Security Considerations:
     * - Validazione contentType per prevenire file malicious
     * - Size limit checking consigliato
     * - Virus scanning per production environment
     * 
     * Memory Management:
     * - File bytes caricati completamente in memory
     * - Considerare streaming per file molto grandi
     * - Cleanup automatico MultipartFile dopo processing
     * 
     * NOTA: Metodo non salva Image nel database
     * - Return entity transient (non persistita)
     * - Calling code responsabile per save via repository
     */
    @Transactional
    public Image saveImageFile(MultipartFile file) throws IOException {
        // Validazione input file
        if (file == null || file.isEmpty()) {
            logger.warn("File immagine non valido o vuoto");
            throw new IllegalArgumentException("File immagine non valido");
        }

        // Creazione entità Image con dati file
        Image image = new Image();
        image.setData(file.getBytes()); // Conversione a byte array
        image.setContentType(file.getContentType()); // MIME type per rendering
        
        // Logging per debugging e audit
        logger.info("Immagine preparata per il salvataggio: size={} bytes, contentType={}",
                file.getBytes().length, file.getContentType());
        return image;
    }
}