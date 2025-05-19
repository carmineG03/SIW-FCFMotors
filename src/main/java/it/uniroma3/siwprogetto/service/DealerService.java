package it.uniroma3.siwprogetto.service;

import it.uniroma3.siwprogetto.model.Dealer;
import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.repository.DealerRepository;
import it.uniroma3.siwprogetto.repository.ProductRepository;
import it.uniroma3.siwprogetto.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            toUpdate.setImagePath(dealer.getImagePath());
            Dealer savedDealer = dealerRepository.save(toUpdate);
            logger.info("Dealer updated: id={}, name={}", savedDealer.getId(), savedDealer.getName());
            return savedDealer;
        } else if (!isUpdate && existingDealer.isPresent()) {
            logger.error("Dealer already exists for user '{}': id={}", username, existingDealer.get().getId());
            throw new IllegalStateException("Un concessionario esiste già per questo utente");
        } else if (!isUpdate && !existingDealer.isPresent()) {
            logger.debug("Creating new dealer for user: id={}, username={}", user.getId(), username);
            dealer.setOwner(user);
            Dealer savedDealer = dealerRepository.save(dealer);
            logger.info("Dealer created: id={}, name={}, owner_id={}",
                    savedDealer.getId(), savedDealer.getName(), savedDealer.getOwner().getId());
            return savedDealer;
        } else {
            logger.error("Invalid state: isUpdate=true but no dealer exists for user '{}'", username);
            throw new IllegalStateException("Nessun concessionario trovato per la modifica");
        }
    }

    public Dealer findByOwner() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.debug("Finding dealer for user: {}", username);

        if (username == null || "anonymousUser".equals(username)) {
            logger.warn("No authenticated user found");
            return null;
        }

        Optional<Dealer> dealer = dealerRepository.findByOwnerUsername(username);
        if (dealer.isPresent()) {
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

        product.setDescription(updatedProduct.getDescription());
        product.setPrice(updatedProduct.getPrice());
        Product savedProduct = productRepository.save(product);
        logger.info("Product updated: id={}, model={}, seller_id={}",
                savedProduct.getId(), savedProduct.getModel(), user.getId());
        return savedProduct;
    }

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
        logger.info("Found {} products for user: id={}", products.size(), user.getId());
        return products;
    }

    public List<Dealer> findByLocation(String query) {
        logger.debug("Finding dealers with query: {}", query);
        if (query == null || query.trim().isEmpty()) {
            return dealerRepository.findAll();
        }
        return dealerRepository.findByAddressContainingIgnoreCase(query);
    }

    public List<Dealer> findAll() {
        logger.info("Retrieving all dealers");
        List<Dealer> dealers = dealerRepository.findAll();
        logger.info("Trovati {} concessionari.", dealers.size());
        return dealers;
    }

    public Product findProductById(Long id) {
        logger.debug("Finding product by ID: {}", id);
        return productRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Product not found: id={}", id);
                    return new IllegalStateException("Prodotto non trovato");
                });
    }

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
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID del concessionario non valido.");
        }
        if (!dealerRepository.existsById(id)) {
            throw new IllegalArgumentException("Concessionario non trovato con ID: " + id);
        }

        Dealer dealer = dealerRepository.findById(id).get();
        List<Product> products = productRepository.findBySeller(dealer.getOwner());
        if (!products.isEmpty()) {
            productRepository.deleteAll(products);
        }

        try {
            Query query = entityManager.createQuery("DELETE FROM Dealer d WHERE d.id = :id");
            query.setParameter("id", id);
            dealerRepository.flush();
            if (dealerRepository.existsById(id)) {
                throw new IllegalStateException("Eliminazione non riuscita: il concessionario esiste ancora.");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Errore durante l'eliminazione del concessionario.", e);
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

    public Dealer findByOwnerUsername(String username) {
        logger.debug("Finding dealer by owner username: {}", username);

        if (username == null || username.trim().isEmpty()) {
            logger.warn("Invalid username provided: {}", username);
            return null;
        }

        Optional<Dealer> dealer = dealerRepository.findByOwnerUsername(username);
        if (dealer.isPresent()) {
            logger.info("Dealer found for username '{}': id={}, name={}", username, dealer.get().getId(), dealer.get().getName());
            return dealer.get();
        } else {
            logger.warn("No dealer found for username '{}'", username);
            return null;
        }
    }
}