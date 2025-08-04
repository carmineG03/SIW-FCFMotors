package it.uniroma3.siwprogetto.service;

import it.uniroma3.siwprogetto.model.Dealer;
import it.uniroma3.siwprogetto.model.Image;
import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.model.QuoteRequest;
import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.repository.DealerRepository;
import it.uniroma3.siwprogetto.repository.ProductRepository;
import it.uniroma3.siwprogetto.repository.QuoteRequestRepository;
import it.uniroma3.siwprogetto.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DealerService {

    private static final Logger logger = LoggerFactory.getLogger(DealerService.class);

    @Autowired
    private DealerRepository dealerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private QuoteRequestRepository quoteRequestRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public Dealer saveDealer(Dealer dealer, boolean isUpdate) {
        logger.info("Saving dealer: name={}, isUpdate={}", dealer.getName(), isUpdate);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.debug("Current authenticated user: {}", username);

        if (username == null || "anonymousUser".equals(username)) {
            logger.error("No authenticated user found");
            throw new IllegalStateException("Utente non autenticato");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("Utente non trovato: {}", username);
                    return new IllegalStateException("Utente non trovato: " + username);
                });

        Optional<Dealer> existingDealer = dealerRepository.findByOwnerUsername(username);
        if (isUpdate && existingDealer.isPresent()) {
            logger.debug("Updating existing dealer: id={}", existingDealer.get().getId());
            Dealer toUpdate = existingDealer.get();
            toUpdate.setName(dealer.getName());
            toUpdate.setDescription(dealer.getDescription());
            toUpdate.setAddress(dealer.getAddress());
            toUpdate.setPhone(dealer.getPhone());
            toUpdate.setEmail(dealer.getEmail());
            if (dealer.getImages() != null && !dealer.getImages().isEmpty()) {
                toUpdate.getImages().clear();
                dealer.getImages().forEach(img -> {
                    img.setDealer(toUpdate);
                    toUpdate.getImages().add(img);
                });
            }
            Dealer savedDealer = dealerRepository.save(toUpdate);
            logger.info("Dealer updated: id={}, name={}", savedDealer.getId(), savedDealer.getName());
            return savedDealer;
        } else if (!isUpdate && existingDealer.isPresent()) {
            logger.error("Dealer already exists for user '{}': id={}", username, existingDealer.get().getId());
            throw new IllegalStateException("Un concessionario esiste già per questo utente");
        } else if (!isUpdate && !existingDealer.isPresent()) {
            logger.debug("Creating new dealer for user: id={}, username={}", user.getId(), username);
            dealer.setOwner(user);
            dealer.getImages().forEach(img -> img.setDealer(dealer));
            Dealer savedDealer = dealerRepository.save(dealer);
            logger.info("Dealer created: id={}, name={}, owner_id={}",
                    savedDealer.getId(), savedDealer.getName(), savedDealer.getOwner().getId());
            return savedDealer;
        } else {
            logger.error("Invalid state: isUpdate=true but no dealer exists for user '{}'", username);
            throw new IllegalStateException("Nessun concessionario trovato per la modifica");
        }
    }

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
            Hibernate.initialize(dealer.get().getImages());
            return dealer.get();
        } else {
            logger.warn("No dealer found for user '{}'", username);
            return null;
        }
    }

    @Transactional
    public Product addProduct(Product product) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.debug("Adding product for user: {}", username);

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

        product.setSeller(user);
        product.setSellerType("DEALER");
        product.getImages().forEach(img -> img.setProduct(product));
        Product savedProduct = productRepository.save(product);
        logger.info("Product added: id={}, model={}, seller_id={}",
                savedProduct.getId(), savedProduct.getModel(), user.getId());
        return savedProduct;
    }

    @Transactional
    public Product updateProduct(Long productId, Product updatedProduct) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.debug("Updating product: id={}, user={}", productId, username);

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

        if (!product.getSeller().getId().equals(user.getId())) {
            logger.error("Product does not belong to user: product_id={}, user_id={}", productId, user.getId());
            throw new IllegalStateException("Il prodotto non appartiene a questo utente");
        }

        product.setModel(updatedProduct.getModel());
        product.setBrand(updatedProduct.getBrand());
        product.setCategory(updatedProduct.getCategory());
        product.setDescription(updatedProduct.getDescription());
        product.setPrice(updatedProduct.getPrice());
        product.setMileage(updatedProduct.getMileage());
        product.setYear(updatedProduct.getYear());
        product.setFuelType(updatedProduct.getFuelType());
        product.setTransmission(updatedProduct.getTransmission());
        if (updatedProduct.getImages() != null && !updatedProduct.getImages().isEmpty()) {
            product.getImages().clear();
            updatedProduct.getImages().forEach(img -> {
                img.setProduct(product);
                product.getImages().add(img);
            });
        }
        product.setIsFeatured(updatedProduct.isFeatured());
        product.setFeaturedUntil(updatedProduct.getFeaturedUntil());
        Product savedProduct = productRepository.save(product);
        logger.info("Product updated: id={}, model={}, seller_id={}",
                savedProduct.getId(), savedProduct.getModel(), user.getId());
        return savedProduct;
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByDealer() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.debug("Retrieving products for user: {}", username);

        if (username == null || "anonymousUser".equals(username)) {
            logger.warn("No authenticated user found");
            return List.of();
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("Utente non trovato: {}", username);
                    return new IllegalStateException("Utente non trovato: " + username);
                });

        Dealer dealer = findByOwner();
        if (dealer == null) {
            logger.warn("No dealer found for user '{}'", username);
            return List.of();
        }

        List<Product> products = productRepository.findBySellerId(user.getId());
        products.forEach(product -> Hibernate.initialize(product.getImages()));
        logger.info("Found {} products for user: id={}", products.size(), user.getId());
        return products;
    }

    @Transactional(readOnly = true)
    public List<Dealer> findByLocation(String query) {
        logger.debug("Finding dealers with query: {}", query);
        if (query == null || query.trim().isEmpty()) {
            return dealerRepository.findAll();
        }
        return dealerRepository.findByAddressContainingIgnoreCase(query);
    }

    @Transactional(readOnly = true)
    public List<Dealer> findAll() {
        logger.info("Retrieving all dealers");
        List<Dealer> dealers = dealerRepository.findAll();
        dealers.forEach(dealer -> Hibernate.initialize(dealer.getImages()));
        logger.info("Trovati {} concessionari.", dealers.size());
        return dealers;
    }

    @Transactional(readOnly = true)
    public Product findProductById(Long id) {
        logger.debug("Finding product by ID: {}", id);
        Optional<Product> product = productRepository.findById(id);
        if (product.isPresent()) {
            Hibernate.initialize(product.get().getImages());
            return product.get();
        } else {
            logger.error("Product not found: id={}", id);
            return null;
        }
    }

    @Transactional
    public void deleteProduct(Long id) {
        logger.debug("Deleting product: id={}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Product not found: id={}", id);
                    return new IllegalStateException("Prodotto non trovato");
                });
        productRepository.delete(product);
        logger.info("Product deleted: id={}", id);
    }

    @Transactional
    public void deleteDealer(Long id) {
        logger.info("Attempting to delete dealer with ID: {}", id);

        if (id == null || id <= 0) {
            logger.error("Invalid dealer ID: {}", id);
            throw new IllegalArgumentException("ID del concessionario non valido.");
        }

        if (!dealerRepository.existsById(id)) {
            logger.error("Dealer not found with ID: {}", id);
            throw new IllegalArgumentException("Concessionario non trovato con ID: " + id);
        }

        try {
            Dealer dealer = dealerRepository.findById(id)
                    .orElseThrow(() -> {
                        logger.error("Dealer not found during retrieval: {}", id);
                        return new IllegalStateException("Concessionario non trovato durante il recupero: " + id);
                    });
            logger.debug("Dealer found: id={}, name={}", dealer.getId(), dealer.getName());

            logger.info("Deleting quote requests associated with products of dealer ID: {}", id);
            Query deleteQuoteRequestsByProductsQuery = entityManager.createQuery(
                    "DELETE FROM QuoteRequest qr WHERE qr.product.seller = :owner"
            );
            deleteQuoteRequestsByProductsQuery.setParameter("owner", dealer.getOwner());
            int quoteRequestsByProductsDeleted = deleteQuoteRequestsByProductsQuery.executeUpdate();
            logger.debug("Deleted {} quote requests associated with products", quoteRequestsByProductsDeleted);
            entityManager.flush();

            logger.info("Deleting products associated with dealer ID: {}", id);
            Query deleteProductsQuery = entityManager.createQuery(
                    "DELETE FROM Product p WHERE p.seller = :owner"
            );
            deleteProductsQuery.setParameter("owner", dealer.getOwner());
            int productsDeleted = deleteProductsQuery.executeUpdate();
            logger.debug("Deleted {} products", productsDeleted);
            entityManager.flush();

            logger.info("Deleting quote requests directly associated with dealer ID: {}", id);
            Query deleteQuoteRequestsByDealerQuery = entityManager.createQuery(
                    "DELETE FROM QuoteRequest qr WHERE qr.dealer.id = :dealerId"
            );
            deleteQuoteRequestsByDealerQuery.setParameter("dealerId", id);
            int quoteRequestsByDealerDeleted = deleteQuoteRequestsByDealerQuery.executeUpdate();
            logger.debug("Deleted {} quote requests directly associated with dealer", quoteRequestsByDealerDeleted);
            entityManager.flush();

            logger.info("Deleting dealer with ID: {}", id);
            Query deleteDealerQuery = entityManager.createQuery(
                    "DELETE FROM Dealer d WHERE d.id = :id"
            );
            deleteDealerQuery.setParameter("id", id);
            int dealersDeleted = deleteDealerQuery.executeUpdate();
            logger.debug("Deleted {} dealers", dealersDeleted);
            entityManager.flush();

            if (dealerRepository.existsById(id)) {
                logger.error("Failed to delete dealer: still exists with ID: {}", id);
                throw new IllegalStateException("Eliminazione non riuscita: il concessionario esiste ancora.");
            }

            logger.info("Dealer with ID: {} deleted successfully", id);
        } catch (DataIntegrityViolationException e) {
            logger.error("Data integrity violation while deleting dealer ID: {}. Possible foreign key constraints.", id, e);
            throw new IllegalStateException("Impossibile eliminare il concessionario: esistono vincoli di integrità (es. entità correlate non eliminate).", e);
        } catch (Exception e) {
            logger.error("Unexpected error while deleting dealer ID: {}", id, e);
            throw new IllegalStateException("Errore durante l'eliminazione del concessionario: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Dealer findByOwnerUsername(String username) {
        logger.debug("Finding dealer by owner username: {}", username);

        if (username == null || username.trim().isEmpty()) {
            logger.warn("Invalid username provided: {}", username);
            return null;
        }

        Optional<Dealer> dealer = dealerRepository.findByOwnerUsername(username);
        if (dealer.isPresent()) {
            logger.info("Dealer found for username '{}': id={}, name={}", username, dealer.get().getId(), dealer.get().getName());
            Hibernate.initialize(dealer.get().getImages());
            return dealer.get();
        } else {
            logger.warn("No dealer found for username '{}'", username);
            return null;
        }
    }

    @Transactional
    public Product highlightProduct(Long productId, int duration) {
        logger.debug("Highlighting product: id={}, duration={}", productId, duration);

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

        if (!product.getSeller().getId().equals(user.getId())) {
            logger.error("Product does not belong to user: product_id={}, user_id={}", productId, user.getId());
            throw new IllegalStateException("Il prodotto non appartiene a questo utente");
        }

        if (duration <= 0) {
            logger.error("Invalid highlight duration: {}", duration);
            throw new IllegalStateException("La durata dell'evidenza deve essere positiva");
        }

        product.setIsFeatured(true);
        product.setFeaturedUntil(LocalDateTime.now().plusDays(duration));
        Product savedProduct = productRepository.save(product);
        logger.info("Product highlighted: id={}, model={}, featuredUntil={}",
                savedProduct.getId(), savedProduct.getModel(), savedProduct.getFeaturedUntil());
        return savedProduct;
    }

    @Transactional
    public Product removeHighlight(Long productId) {
        logger.debug("Removing highlight from product: id={}", productId);

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

        if (!product.getSeller().getId().equals(user.getId())) {
            logger.error("Product does not belong to user: product_id={}, user_id={}", productId, user.getId());
            throw new IllegalStateException("Il prodotto non appartiene a questo utente");
        }

        product.setIsFeatured(false);
        product.setFeaturedUntil(null);
        Product savedProduct = productRepository.save(product);
        logger.info("Highlight removed from product: id={}, model={}", savedProduct.getId(), savedProduct.getModel());
        return savedProduct;
    }

    @Transactional(readOnly = true)
    public Dealer findById(Long id) {
        logger.debug("Finding dealer by ID: {}", id);
        Optional<Dealer> dealer = dealerRepository.findById(id);
        if (dealer.isPresent()) {
            Hibernate.initialize(dealer.get().getImages());
            return dealer.get();
        } else {
            logger.warn("No dealer found for ID '{}'", id);
            return null;
        }
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByDealerOwner(Dealer dealer) {
        if (dealer == null) {
            throw new IllegalArgumentException("Concessionario non valido.");
        }
        if (dealer.getOwner() == null) {
            throw new IllegalStateException("Il concessionario non ha un proprietario associato.");
        }
        List<Product> products = productRepository.findBySeller(dealer.getOwner());
        products.forEach(product -> Hibernate.initialize(product.getImages()));
        return products;
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

    @Transactional(readOnly = true)
    public User getAuthenticatedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.debug("Retrieving authenticated user: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Utente autenticato non trovato: " + username));
    }
}