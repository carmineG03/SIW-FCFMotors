package it.uniroma3.siwprogetto.repository;

import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository per gestione prodotti automotive (Product)
 * Estende CrudRepository per operazioni base e aggiunge query complesse JPQL
 * 
 * Responsabilità:
 * - Accesso dati catalogo prodotti automotive
 * - Query di ricerca avanzata con filtri multipli
 * - Gestione prodotti evidenziati/featured
 * - Relazioni Product-User (seller) e Product-Category
 * - Operazioni aggregate per analytics
 * 
 * Pattern Repository vantaggi:
 * - Query JPQL ottimizzate per ricerche complesse
 * - Named parameters per sicurezza SQL injection
 * - Gestione automatica paginazione future
 * - Type-safety su BigDecimal per prezzi
 * 
 * Business Domain:
 * - Product = veicolo in vendita con caratteristiche tecniche
 * - Featured products = evidenziati con subscription premium
 * - Filtri avanzati per ricerca veicoli specifici
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
@Repository
public interface ProductRepository extends CrudRepository<Product, Long> {

    /**
     * Override di findAll() per documentazione specifica
     * Eredita da CrudRepository<Product, Long>
     * 
     * @return Lista completa di tutti i prodotti nel catalogo
     * 
     * Utilizzi:
     * - Caricamento catalogo completo (con paginazione futura)
     * - Export dati per analytics
     * - Dashboard amministrativa
     * 
     * Performance:
     * - Query potenzialmente costosa per grandi cataloghi
     * - Considerare findAllOrderedByHighlight() per UX migliore
     */
    List<Product> findAll();

    /**
     * Trova prodotti per categoria specifica
     * Query JPQL: SELECT p FROM Product p WHERE p.category = :category
     * 
     * @param category Categoria prodotto (es: "AUTO", "MOTO", "CAMPER")
     * @return Lista prodotti della categoria specificata
     * 
     * Utilizzi:
     * - Navigazione catalogo per categoria
     * - Filtri rapidi homepage
     * - Statistiche inventory per categoria
     * 
     * Index consigliato:
     * - Su colonna 'category' per performance
     * - Valori limitati ideali per B-Tree index
     */
    @Query("SELECT p FROM Product p WHERE p.category = :category")
    List<Product> findByCategory(@Param("category") String category);

    /**
     * Trova tutte le categorie distinte presenti nel catalogo
     * Query JPQL: SELECT DISTINCT p.category FROM Product p
     * 
     * @return Lista categorie univoche esistenti (non null)
     * 
     * Utilizzi:
     * - Popolamento dropdown filtri categoria
     * - Menu navigazione dinamico
     * - Validazione categorie ammesse
     * 
     * Performance:
     * - DISTINCT può essere costoso su grandi dataset
     * - Cache risultato se categorie cambiano raramente
     */
    @Query("SELECT DISTINCT p.category FROM Product p")
    List<String> findAllCategories();

    /**
     * Trova tutti i brand distinti presenti nel catalogo
     * Query JPQL: SELECT DISTINCT p.brand FROM Product p
     * 
     * @return Lista brand/marche univoche esistenti
     * 
     * Utilizzi:
     * - Dropdown selezione marca in filtri
     * - Analytics distribuzione brand
     * - Validazione brand ammessi
     */
    @Query("SELECT DISTINCT p.brand FROM Product p")
    List<String> findAllBrands();

    /**
     * Trova modelli distinti per brand specifico
     * Query JPQL con filtro: SELECT DISTINCT p.model FROM Product p WHERE p.brand = :brand
     * 
     * @param brand Marca per cui cercare i modelli disponibili
     * @return Lista modelli univoci del brand specificato
     * 
     * Utilizzi:
     * - Cascading dropdown: prima brand, poi modello
     * - UX guidata nella selezione veicolo
     * - Validazione combinazione brand-model
     * 
     * Relazione:
     * - Brand 1:N Model (un brand ha più modelli)
     * - Dipendenza funzionale Brand -> Model
     */
    @Query("SELECT DISTINCT p.model FROM Product p WHERE p.brand = :brand")
    List<String> findModelsByBrand(@Param("brand") String brand);

    /**
     * Trova tipi di carburante distinti nel catalogo
     * Query JPQL: SELECT DISTINCT p.fuelType FROM Product p
     * 
     * @return Lista tipi carburante univoci (es: "BENZINA", "DIESEL", "ELETTRICO", "IBRIDO")
     * 
     * Utilizzi:
     * - Filtro ricerca per tipo alimentazione
     * - Statistiche distribuzione green/tradizionale
     * - Compliance normative ambientali
     */
    @Query("SELECT DISTINCT p.fuelType FROM Product p")
    List<String> findAllFuelTypes();

    /**
     * Trova tipi di trasmissione distinti nel catalogo
     * Query JPQL: SELECT DISTINCT p.transmission FROM Product p
     * 
     * @return Lista tipi trasmissione univoci (es: "MANUALE", "AUTOMATICO", "SEMIAUTOMATICO")
     * 
     * Utilizzi:
     * - Filtro ricerca per tipo cambio
     * - Preferenze utente driving experience
     * - Analytics domanda mercato trasmissioni
     */
    @Query("SELECT DISTINCT p.transmission FROM Product p")
    List<String> findAllTransmissions();

    /**
     * Ricerca avanzata prodotti con filtri multipli opzionali
     * Query JPQL complessa con AND condizionali per ogni parametro
     * 
     * Pattern: (:param IS NULL OR field = :param) per filtri opzionali
     * - Se parametro è null, condizione ignorata
     * - Se parametro valorizzato, applicato come filtro
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
     * @return Lista prodotti che matchano tutti i filtri specificati
     * 
     * Utilizzi:
     * - Ricerca avanzata homepage con form multiplo
     * - API REST con query parameters opzionali
     * - Salvataggio ricerche preferite utente
     * 
     * Performance:
     * - Index multipli consigliati per campi più utilizzati
     * - Query optimizer utilizza statistiche per execution plan
     * - Parametri null riducono complessità query
     */
    @Query("SELECT p FROM Product p WHERE " +
            "(:category IS NULL OR p.category = :category) " +
            "AND (:brand IS NULL OR p.brand = :brand) " +
            "AND (:selectedModel IS NULL OR p.model = :selectedModel) " +
            "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
            "AND (:minMileage IS NULL OR p.mileage >= :minMileage) " +
            "AND (:maxMileage IS NULL OR p.mileage <= :maxMileage) " +
            "AND (:minYear IS NULL OR p.year >= :minYear) " +
            "AND (:maxYear IS NULL OR p.year <= :maxYear) " +
            "AND (:fuelType IS NULL OR p.fuelType = :fuelType) " +
            "AND (:transmission IS NULL OR p.transmission = :transmission)")
    List<Product> findByFilters(@Param("category") String category,
                                @Param("brand") String brand,
                                @Param("selectedModel") String selectedModel,
                                @Param("minPrice") BigDecimal minPrice,
                                @Param("maxPrice") BigDecimal maxPrice,
                                @Param("minMileage") Integer minMileage,
                                @Param("maxMileage") Integer maxMileage,
                                @Param("minYear") Integer minYear,
                                @Param("maxYear") Integer maxYear,
                                @Param("fuelType") String fuelType,
                                @Param("transmission") String transmission);

    /**
     * Trova prodotti di un venditore specifico tramite ID
     * Query JPQL: SELECT p FROM Product p WHERE p.seller.id = :sellerId
     * 
     * @param sellerId ID del venditore/utente proprietario
     * @return Lista prodotti in vendita dal seller specificato
     * 
     * Utilizzi:
     * - Dashboard venditore con i propri annunci
     * - Gestione inventory personale
     * - Statistiche vendite per seller
     * - Moderazione contenuti per utente specifico
     */
    @Query("SELECT p FROM Product p WHERE p.seller.id = :sellerId")
    List<Product> findBySellerId(@Param("sellerId") Long sellerId);

    /**
     * Trova prodotti di un venditore tramite oggetto User
     * Query generata automaticamente: SELECT * FROM product WHERE seller_id = ?
     * 
     * @param user Oggetto User venditore
     * @return Lista prodotti dell'utente venditore
     * 
     * Utilizzi:
     * - Alternativa a findBySellerId quando si ha oggetto User
     * - Navigazione relazioni JPA più diretta
     * - Lazy loading gestito automaticamente
     */
    List<Product> findBySeller(User user);

    /**
     * Trova prodotti evidenziati attualmente attivi
     * Query JPQL con condizioni temporali:
     * - isFeatured = true (prodotto marked as featured)
     * - featuredUntil IS NULL (featured permanente) OR featuredUntil > NOW (non scaduto)
     * - Ordinamento per scadenza DESC (scadenza più lontana prima)
     * 
     * @return Lista prodotti featured attivi ordinati per scadenza
     * 
     * Utilizzi:
     * - Homepage con prodotti in evidenza
     * - Carousel prodotti premium
     * - Revenue da subscription premium sellers
     * 
     * Business Logic:
     * - Featured products visibili in posizioni premium
     * - Scadenza automatica per subscription temporanee
     * - Sellers premium ottengono maggiore visibilità
     */
    @Query("SELECT p FROM Product p WHERE p.isFeatured = true AND (p.featuredUntil IS NULL OR p.featuredUntil > CURRENT_TIMESTAMP) ORDER BY p.featuredUntil DESC")
    List<Product> findActiveHighlightedProducts();

    /**
     * Trova tutti i prodotti con priorità per quelli evidenziati
     * Query JPQL con ORDER BY condizionale:
     * - CASE WHEN per assegnare priorità 0 ai featured attivi, 1 agli altri
     * - Prodotti featured mostrati per primi
     * - Ordinamento secondario per ID DESC (più recenti primi)
     * 
     * @return Lista completa prodotti ordinati per evidenziazione
     * 
     * Utilizzi:
     * - Listino generale con featured products in cima
     * - UX che privilenzia sellers premium
     * - Balance tra contenuti gratuiti e premium
     * 
     * UX Strategy:
     * - Featured products catturano attenzione immediata
     * - Contenuti gratuiti comunque accessibili sotto
     * - Incentivo per sellers ad acquistare evidenziazione
     */
    @Query("SELECT p FROM Product p ORDER BY CASE WHEN p.isFeatured = true AND (p.featuredUntil IS NULL OR p.featuredUntil > CURRENT_TIMESTAMP) THEN 0 ELSE 1 END, p.id DESC")
    List<Product> findAllOrderedByHighlight();

    /**
     * Elimina prodotto per ID
     * Override del metodo base per documentazione specifica
     * 
     * ATTENZIONE: Operazione irreversibile
     * - Eliminazione fisica dal database
     * - Perdita dati prodotto e storico
     * - Verificare dipendenze (QuoteRequest, UserSubscription)
     * 
     * @param id ID del prodotto da eliminare
     * 
     * Casi d'uso:
     * - Rimozione annuncio da parte del venditore
     * - Moderazione contenuti inappropriati
     * - Cleanup prodotti obsoleti o duplicati
     * 
     * Cascading:
     * - Configurare @OnDelete per relazioni dipendenti
     * - Soft delete alternativo per audit trail
     */
    void deleteById(Long id);

    /**
     * Conta prodotti featured di un venditore specifico
     * Query JPQL con COUNT e condizioni:
     * - WHERE seller = :seller AND isFeatured = true
     * - Ritorna long per supportare grandi numeri
     * 
     * @param seller Oggetto User venditore
     * @return Numero di prodotti featured del venditore
     * 
     * Utilizzi:
     * - Controllo limiti subscription (es: max 5 featured per piano Basic)
     * - Dashboard venditore con statistiche
     * - Business logic per upgrade subscription
     * - Billing verification per addebiti
     * 
     * Performance:
     * - Query COUNT(*) ottimizzata
     * - Index su (seller_id, is_featured) consigliato
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.seller = :seller AND p.isFeatured = true")
    long countBySellerAndIsFeaturedTrue(@Param("seller") User seller);
}