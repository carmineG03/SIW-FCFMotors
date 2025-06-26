package it.uniroma3.siwprogetto.service;

import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.model.Subscription;
import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.repository.ProductRepository;
import it.uniroma3.siwprogetto.repository.UserRepository;
import it.uniroma3.siwprogetto.repository.UserSubscriptionRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserSubscriptionRepository userSubscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class); // Fixed: Use ProductService logger

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // Trova tutti i prodotti
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    // Trova prodotti per categoria
    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    // Trova tutte le categorie
    public List<String> findAllCategories() {
        return productRepository.findAllCategories();
    }

    // Trova tutte le marche
    public List<String> findAllBrands() {
        return productRepository.findAll().stream()
                .map(Product::getBrand)
                .distinct()
                .collect(Collectors.toList());
    }

    // Trova modelli per marca
    public List<String> findModelsByBrand(String brand) {
        return productRepository.findModelsByBrand(brand);
    }

    // Trova tutti i tipi di carburante
    public List<String> findAllFuelTypes() {
        return productRepository.findAllFuelTypes();
    }

    // Trova tutte le trasmissioni
    public List<String> findAllTransmissions() {
        return productRepository.findAllTransmissions();
    }

    // Trova prodotti con filtri
    public List<Product> findByFilters(String category, String brand, String selectedModel,
                                       BigDecimal minPrice, BigDecimal maxPrice,
                                       Integer minMileage, Integer maxMileage,
                                       Integer minYear, Integer maxYear,
                                       String fuelType, String transmission, String query) {
        logger.debug("Filtri applicati: category={}, brand={}, model={}, minPrice={}, maxPrice={}, minMileage={}, maxMileage={}, minYear={}, maxYear={}, fuelType={}, transmission={}, query={}",
                category, brand, selectedModel, minPrice, maxPrice, minMileage, maxMileage, minYear, maxYear, fuelType, transmission, query);

        // Fetch products using the repository without the query parameter
        List<Product> results = productRepository.findByFilters(category, brand, selectedModel,
                minPrice, maxPrice, minMileage, maxMileage,
                minYear, maxYear, fuelType, transmission);

        // Apply query filter in Java if query is not null or empty
        if (query != null && !query.trim().isEmpty()) {
            // Split the query into terms (e.g., "Fiat 500" -> ["fiat", "500"])
            String[] searchTerms = query.trim().toLowerCase().split("\\s+");
            logger.debug("Termini di ricerca: {}", Arrays.toString(searchTerms));

            results = results.stream()
                    .filter(p -> {
                        // Check if all terms match in brand, model, or category
                        for (String term : searchTerms) {
                            boolean termMatches = (p.getBrand() != null && p.getBrand().toLowerCase().contains(term)) ||
                                    (p.getModel() != null && p.getModel().toLowerCase().contains(term)) ||
                                    (p.getCategory() != null && p.getCategory().toLowerCase().contains(term));
                            if (!termMatches) {
                                return false; // If any term doesn't match, exclude the product
                            }
                        }
                        return true; // All terms match
                    })
                    .collect(Collectors.toList());
        }

        logger.debug("Trovati {} prodotti", results.size());
        results.forEach(p -> logger.debug("Prodotto: id={}, brand={}, model={}", p.getId(), p.getBrand(), p.getModel()));
        return results;
    }

    // Trova un prodotto per ID
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    public Product findById1(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    // Salva un prodotto
    public Product save(Product product) {
        return productRepository.save(product);
    }

    // Elimina un prodotto per ID
    public void deleteById(Long id) {
        productRepository.deleteById(id);
    }

    // Trova prodotti per ID del venditore
    public List<Product> findBySellerId(Long sellerId) {
        return productRepository.findBySellerId(sellerId);
    }

    public boolean canAddFeaturedCar(User user, Product product) {
        User fullUser = userRepository.findById(user.getId())
                .orElseThrow(() -> {
                    logger.error("Utente non trovato: {}", user.getId());
                    return new IllegalStateException("Utente non trovato");
                });
        Subscription subscription = fullUser.getSubscription();
        if (subscription == null) {
            logger.warn("Nessun abbonamento trovato per l'utente: {}", fullUser.getUsername());
            return false;
        }

        int maxFeaturedProducts = subscription.getMaxFeaturedCars();
        logger.debug("Limite massimo di prodotti in evidenza per l'utente {}: {}", fullUser.getUsername(), maxFeaturedProducts);
        if (maxFeaturedProducts <= 0) {
            logger.warn("maxFeaturedProducts non valido per l'utente {}: {}", fullUser.getUsername(), maxFeaturedProducts);
            return false;
        }

        long featuredCount = productRepository.countBySellerAndIsFeaturedTrue(user);
        logger.debug("Prodotti attualmente in evidenza per l'utente {}: {}", fullUser.getUsername(), featuredCount);

        if (product != null && product.isFeatured()) {
            featuredCount--;
            logger.debug("Escludendo il prodotto corrente, conteggio aggiornato: {}", featuredCount);
        }

        boolean canAdd = featuredCount < maxFeaturedProducts;
        logger.info("L'utente {} può aggiungere un prodotto in evidenza: {}", fullUser.getUsername(), canAdd);
        return canAdd;
    }

    @Transactional
    public void setProductFeatured(Long id, boolean isFeatured, LocalDateTime featuredUntil) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Prodotto non trovato"));
        User user = product.getSeller();
        if (isFeatured && !canAddFeaturedCar(user, product)) {
            throw new IllegalStateException("Limite massimo di prodotti in evidenza raggiunto per il tuo abbonamento");
        }
        product.setIsFeatured(isFeatured);
        product.setFeaturedUntil(featuredUntil);
        productRepository.save(product);
    }


}