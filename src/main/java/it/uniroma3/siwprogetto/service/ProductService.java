package it.uniroma3.siwprogetto.service;

import it.uniroma3.siwprogetto.model.Image;
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
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Transactional
    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    @Transactional
    public List<String> findAllCategories() {
        return productRepository.findAllCategories();
    }

    @Transactional
    public List<String> findAllBrands() {
        return productRepository.findAll().stream()
                .map(Product::getBrand)
                .distinct()
                .collect(Collectors.toList());
    }

    @Transactional
    public List<String> findModelsByBrand(String brand) {
        return productRepository.findModelsByBrand(brand);
    }

    @Transactional
    public List<String> findAllFuelTypes() {
        return productRepository.findAllFuelTypes();
    }

    @Transactional
    public List<String> findAllTransmissions() {
        return productRepository.findAllTransmissions();
    }

    @Transactional
    public List<Product> findByFilters(String category, String brand, String selectedModel,
                                       BigDecimal minPrice, BigDecimal maxPrice,
                                       Integer minMileage, Integer maxMileage,
                                       Integer minYear, Integer maxYear,
                                       String fuelType, String transmission, String query) {
        logger.debug("Filtri applicati: category={}, brand={}, model={}, minPrice={}, maxPrice={}, minMileage={}, maxMileage={}, minYear={}, maxYear={}, fuelType={}, transmission={}, query={}",
                category, brand, selectedModel, minPrice, maxPrice, minMileage, maxMileage, minYear, maxYear, fuelType, transmission, query);

        List<Product> results = productRepository.findByFilters(category, brand, selectedModel,
                minPrice, maxPrice, minMileage, maxMileage,
                minYear, maxYear, fuelType, transmission);

        if (query != null && !query.trim().isEmpty()) {
            String[] searchTerms = query.trim().toLowerCase().split("\\s+");
            logger.debug("Termini di ricerca: {}", Arrays.toString(searchTerms));

            results = results.stream()
                    .filter(p -> {
                        for (String term : searchTerms) {
                            boolean termMatches = (p.getBrand() != null && p.getBrand().toLowerCase().contains(term)) ||
                                    (p.getModel() != null && p.getModel().toLowerCase().contains(term)) ||
                                    (p.getCategory() != null && p.getCategory().toLowerCase().contains(term));
                            if (!termMatches) {
                                return false;
                            }
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        }

        logger.debug("Trovati {} prodotti", results.size());
        results.forEach(p -> logger.debug("Prodotto: id={}, brand={}, model={}", p.getId(), p.getBrand(), p.getModel()));
        return results;
    }

    @Transactional
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    @Transactional
    public Product findById1(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    @Transactional
    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    public void deleteById(Long id) {
        productRepository.deleteById(id);
    }

    @Transactional
    public List<Product> findBySellerId(Long sellerId) {
        return productRepository.findBySellerId(sellerId);
    }

    @Transactional
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

    @Transactional
    public Image saveImageFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            logger.warn("File immagine non valido o vuoto");
            throw new IllegalArgumentException("File immagine non valido");
        }

        Image image = new Image();
        image.setData(file.getBytes());
        image.setContentType(file.getContentType());
        logger.info("Immagine preparata per il salvataggio: size={} bytes, contentType={}",
                file.getBytes().length, file.getContentType());
        return image;
    }
}